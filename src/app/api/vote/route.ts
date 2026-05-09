import { NextResponse } from "next/server";
import { createServerClient } from "@supabase/auth-helpers-nextjs";
import { createClient } from "@supabase/supabase-js";
import { cookies } from "next/headers";
import type { Database } from "@/types";

function getAdminSupabase() {
  return createClient<Database>(
    process.env.NEXT_PUBLIC_SUPABASE_URL || "",
    process.env.SUPABASE_SERVICE_ROLE_KEY || "",
    {
      auth: { persistSession: false },
    }
  );
}

export async function POST(request: Request) {
  const body = await request.json();
  const { questionId, vote, questionText } = body;

  if (!questionId || typeof vote !== "boolean") {
    return NextResponse.json({ error: "Missing vote payload." }, { status: 400 });
  }

  const supabase = createServerClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL || "",
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || "",
    { cookies: cookies() }
  ) as any;
  const {
    data: { session },
  } = await supabase.auth.getSession();

  if (!session) {
    return NextResponse.json({ error: "Unauthorized." }, { status: 401 });
  }

  const questionExists = await supabase.from("questions").select("id").eq("id", questionId).single();

  if (!questionExists.data && questionText) {
    await getAdminSupabase().from("questions").upsert({ id: questionId, text: questionText, is_active: true } as any);
  }

  const { error } = await supabase.from("votes").insert({
    user_id: session.user.id,
    question_id: questionId,
    vote,
  });

  if (error) {
    return NextResponse.json({ error: error.message }, { status: 400 });
  }

  return NextResponse.json({ success: true });
}
