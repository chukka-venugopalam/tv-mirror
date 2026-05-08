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

export async function GET(request: Request) {
  const url = new URL(request.url);
  const questionId = url.searchParams.get("questionId");

  if (!questionId) {
    return NextResponse.json({ error: "Missing questionId." }, { status: 400 });
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

  const { data, error } = await adminSupabase.from("votes").select("question_id, vote");
  if (error) {
    return NextResponse.json({ error: error.message }, { status: 500 });
  }

  const votes = data.filter((item) => item.question_id === questionId);
  const agree = votes.filter((item) => item.vote).length;
  const disagree = votes.length - agree;

  return NextResponse.json({ agree, disagree, total: votes.length });
}
