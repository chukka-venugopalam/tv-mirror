import { SupabaseClient } from "@supabase/supabase-js";
import type { Database, Question, Profile, Vote } from "@/types";

export async function getActiveQuestion(supabase: SupabaseClient<Database>) {
  const { data, error } = await supabase
    .from("questions")
    .select("*")
    .eq("is_active", true)
    .single();

  if (error || !data) {
    // Fallback to local questions if no active DB question
    const { questions } = await import("@/data/questions");
    return questions.find(q => q.isActive) || null;
  }

  return data;
}

export async function getActiveQuestions(supabase: SupabaseClient<Database>) {
  const { data, error } = await supabase
    .from("questions")
    .select("*")
    .eq("is_active", true);

  if (error || !data || data.length === 0) {
    // Fallback to local questions if no active DB questions
    const { questions } = await import("@/data/questions");
    return questions.filter(q => q.isActive) as any;
  }

  return data as any;
}

export async function getProfile(supabase: SupabaseClient<Database>, userId: string) {
  const { data, error } = await supabase
    .from("profiles")
    .select("*")
    .eq("id", userId)
    .single();

  if (error || !data) {
    return null;
  }

  return data as any;
}

export async function getUserVote(supabase: SupabaseClient<Database>, questionId: string, userId: string) {
  const { data, error } = await supabase
    .from("votes")
    .select("*")
    .eq("question_id", questionId)
    .eq("user_id", userId)
    .single();

  if (error) {
    return null;
  }

  return data as Vote;
}

export function getServerSupabase() {
  // This function is kept for compatibility but not used in new pattern
  return null;
}