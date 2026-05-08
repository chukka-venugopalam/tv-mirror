// @ts-nocheck
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
  if (!session || session.user.email !== process.env.NEXT_PUBLIC_ADMIN_EMAIL) {
    return null;
  }
  return session;
}

export async function GET() {
  const session = await requireAdmin();
  if (!session) {
    return NextResponse.json({ error: "Unauthorized." }, { status: 401 });
  }

  const questionsResult = await adminSupabase.from("questions").select("id, text, is_active, created_at, votes(id)");
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
    votes?: Array<{ id: string }>;
  }>;

  const mappedQuestions = questions.map((question) => ({
    id: question.id,
    text: question.text,
    is_active: question.is_active,
    created_at: question.created_at,
    voteCount: question.votes?.length ?? 0,
  }));

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
    const updateData: Record<string, unknown> = { is_active: false };
    await adminSupabase.from("questions").update(updateData).neq("is_active", false);
  }

  const insertData: Record<string, unknown> = { text, is_active: Boolean(isActive) };
  const result = await adminSupabase.from("questions").insert(insertData).select().single();
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
    const updateData: Record<string, unknown> = { is_active: false };
    await adminSupabase.from("questions").update(updateData).neq("is_active", false);
    const toggleData: Record<string, unknown> = { is_active: true };
    const result = await adminSupabase.from("questions").update(toggleData).eq("id", id);
    if (result.error) {
      return NextResponse.json({ error: result.error.message }, { status: 500 });
    }
    return NextResponse.json({ success: true });
  }

  if (action === "update") {
    if (!text) {
      return NextResponse.json({ error: "Question text is required." }, { status: 400 });
    }
    const updateData: Record<string, unknown> = { text };
    const result = await adminSupabase.from("questions").update(updateData).eq("id", id);
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
