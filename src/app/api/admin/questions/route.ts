import { NextResponse } from "next/server";
import { createServerClient } from "@supabase/auth-helpers-nextjs";
import { createClient } from "@supabase/supabase-js";
import { cookies } from "next/headers";
import type { Database } from "@/types";

const adminSupabase = createClient<Database>(
  process.env.NEXT_PUBLIC_SUPABASE_URL || "",
  process.env.SUPABASE_SERVICE_ROLE_KEY || "",
  {
    auth: { persistSession: false },
  }
);

async function requireAdmin() {
  const supabase = createServerClient<Database>(
    process.env.NEXT_PUBLIC_SUPABASE_URL || "",
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || "",
    { cookies: cookies() }
  );

  const {
    data: { session },
  } = await supabase.auth.getSession();

  if (!session) {
    return null;
  }

  const adminEmail = process.env.NEXT_PUBLIC_ADMIN_EMAIL?.toLowerCase();
  if (!session.user.email || session.user.email.toLowerCase() !== adminEmail) {
    return null;
  }

  return session;
}

export async function GET() {
  const session = await requireAdmin();
  if (!session) {
    return NextResponse.json({ error: "Unauthorized." }, { status: 401 });
  }

  const questionsResult = await adminSupabase.from("questions").select("id, text, is_active, created_at, votes(id, vote)");
  if (questionsResult.error) {
    return NextResponse.json({ error: questionsResult.error.message }, { status: 500 });
  }

  const usersCountResult = await adminSupabase.from("profiles").select("id", { count: "exact", head: true });
  const votesCountResult = await adminSupabase.from("votes").select("id", { count: "exact", head: true });

  if (usersCountResult.error || votesCountResult.error) {
    return NextResponse.json({ error: "Unable to load stats." }, { status: 500 });
  }

  const questions = (questionsResult.data ?? []) as Array<{
    id: string;
    text: string;
    is_active: boolean;
    created_at: string;
    votes?: Array<{ id: string; vote: boolean }>;
  }>;

  const mappedQuestions = questions.map((question) => {
    const agreeCount = question.votes?.filter((vote) => vote.vote === true).length ?? 0;
    const disagreeCount = question.votes?.filter((vote) => vote.vote === false).length ?? 0;

    return {
      id: question.id,
      text: question.text,
      is_active: question.is_active,
      created_at: question.created_at,
      voteCount: question.votes?.length ?? 0,
      agreeCount,
      disagreeCount,
    };
  });

  return NextResponse.json({
    questions: mappedQuestions,
    totalUsers: usersCountResult.count || 0,
    totalVotes: votesCountResult.count || 0,
  });
}

export async function POST(request: Request) {
  const session = await requireAdmin();
  if (!session) {
    return NextResponse.json({ error: "Unauthorized." }, { status: 401 });
  }

  const body = await request.json();
  const { text, isActive } = body;

  if (!text) {
    return NextResponse.json({ error: "Question text is required." }, { status: 400 });
  }

  if (isActive) {
    await adminSupabase.from("questions").update({ is_active: false } as any).neq("is_active", false);
  }

  const result = await adminSupabase.from("questions").insert({ text, is_active: Boolean(isActive) }).select().single();
  if (result.error) {
    return NextResponse.json({ error: result.error.message }, { status: 500 });
  }

  return NextResponse.json({ question: result.data });
}

export async function PATCH(request: Request) {
  const session = await requireAdmin();
  if (!session) {
    return NextResponse.json({ error: "Unauthorized." }, { status: 401 });
  }

  const body = await request.json();
  const { id, action, text } = body;

  if (!id || !action) {
    return NextResponse.json({ error: "Invalid request." }, { status: 400 });
  }

  if (action === "toggle") {
    await adminSupabase.from("questions").update({ is_active: false }).neq("is_active", false);
    const result = await adminSupabase.from("questions").update({ is_active: true }).eq("id", id);
    if (result.error) {
      return NextResponse.json({ error: result.error.message }, { status: 500 });
    }
    return NextResponse.json({ success: true });
  }

  if (action === "update") {
    if (!text) {
      return NextResponse.json({ error: "Question text is required." }, { status: 400 });
    }
    const result = await adminSupabase.from("questions").update({ text }).eq("id", id);
    if (result.error) {
      return NextResponse.json({ error: result.error.message }, { status: 500 });
    }
    return NextResponse.json({ success: true });
  }

  return NextResponse.json({ error: "Unknown action." }, { status: 400 });
}

export async function DELETE(request: Request) {
  const session = await requireAdmin();
  if (!session) {
    return NextResponse.json({ error: "Unauthorized." }, { status: 401 });
  }

  const id = new URL(request.url).searchParams.get("id");
  if (!id) {
    return NextResponse.json({ error: "Missing question id." }, { status: 400 });
  }

  const result = await adminSupabase.from("questions").delete().eq("id", id);
  if (result.error) {
    return NextResponse.json({ error: result.error.message }, { status: 500 });
  }

  return NextResponse.json({ success: true });
}
