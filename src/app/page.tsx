"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { createBrowserClient } from "@supabase/auth-helpers-nextjs";
import type { Database, Profile, Question, Vote } from "@/types";
import VoteButtons from "@/components/VoteButtons";
import ResultsBar from "@/components/ResultsBar";
import { Navbar } from "@/components/Navbar";
import { Card } from "@/components/Card";

export default function HomePage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [activeQuestion, setActiveQuestion] = useState<Question | null>(null);
  const [vote, setVote] = useState<Vote | null>(null);
  const [hasVoted, setHasVoted] = useState(false);

  useEffect(() => {
    const supabase = createBrowserClient<Database>(
      process.env.NEXT_PUBLIC_SUPABASE_URL || "",
      process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || ""
    );

    async function initialize() {
      const {
        data: { session },
      } = await supabase.auth.getSession();

      if (!session) {
        router.replace("/login");
        return;
      }

      const userId = session.user.id;
      const { data: profileData } = await supabase
        .from("profiles")
        .select("id, branch, year, goal")
        .eq("id", userId)
        .maybeSingle();

      if (!profileData || !profileData.branch || !profileData.year || !profileData.goal) {
        router.replace("/onboarding");
        return;
      }

      setProfile(profileData);

      const { data: activeQuestionData } = await supabase
        .from("questions")
        .select("id, text, is_active, created_at")
        .eq("is_active", true)
        .limit(1)
        .maybeSingle();

      if (activeQuestionData) {
        setActiveQuestion(activeQuestionData);
        const { data: voteData } = await supabase
          .from("votes")
          .select("vote")
          .eq("question_id", activeQuestionData.id)
          .eq("user_id", userId)
          .maybeSingle();

        setVote(voteData as Vote | null);
        setHasVoted(voteData?.vote === true || voteData?.vote === false);
      }

      setLoading(false);
    }

    initialize();
  }, [router]);

  if (loading) {
    return (
      <>
        <Navbar />
        <main className="min-h-screen bg-gradient-to-br from-white via-blue-50 to-indigo-100 text-slate-900 dark:from-slate-900 dark:via-slate-800 dark:to-blue-900 dark:text-slate-50">
          <div className="mx-auto flex max-w-3xl flex-col gap-8 px-4 py-12 sm:px-6 lg:px-8">
            <div className="rounded-3xl border border-slate-200 bg-white p-12 text-center text-slate-600 shadow-sm dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300">
              Loading your PulseVote experience…
            </div>
          </div>
        </main>
      </>
    );
  }

  return (
    <>
      <Navbar />
      <main className="min-h-screen bg-gradient-to-br from-white via-blue-50 to-indigo-100 text-slate-900 dark:from-slate-900 dark:via-slate-800 dark:to-blue-900 dark:text-slate-50">
        <div className="mx-auto flex max-w-3xl flex-col gap-8 px-4 py-12 sm:px-6 lg:px-8">
          <div className="text-center">
            <p className="text-sm font-semibold uppercase tracking-wider text-indigo-600 dark:text-cyan-400">
              PulseVote
            </p>
            <h1 className="mt-3 text-4xl font-bold tracking-tight sm:text-5xl">
              Today's opinion check
            </h1>
            <p className="mt-2 text-lg text-slate-600 dark:text-slate-300">
              {profile?.branch} · Year {profile?.year}
            </p>
          </div>

          {activeQuestion ? (
            <Card className="p-8">
              <div className="space-y-8">
                <div>
                  <p className="text-xs font-bold uppercase tracking-widest text-indigo-600 dark:text-cyan-400">
                    Today's Question
                  </p>
                  <p className="mt-4 text-3xl font-bold leading-tight">
                    {activeQuestion.text}
                  </p>
                </div>

                <div className="space-y-6">
                  {!hasVoted ? (
                    <VoteButtons questionId={activeQuestion.id} questionText={activeQuestion.text} />
                  ) : (
                    <div className="rounded-lg bg-indigo-50 p-6 dark:bg-slate-700">
                      <p className="text-xs font-bold uppercase tracking-widest text-indigo-600 dark:text-cyan-400">
                        Your Vote
                      </p>
                      <p className="mt-3 text-2xl font-bold text-slate-900 dark:text-slate-50">
                        {vote?.vote ? "✓ Agree" : "✗ Disagree"}
                      </p>
                    </div>
                  )}
                  {hasVoted && <ResultsBar questionId={activeQuestion.id} />}
                </div>
              </div>
            </Card>
          ) : (
            <Card className="p-12 text-center">
              <p className="text-lg font-semibold">No active question available yet.</p>
              <p className="mt-2 text-slate-600 dark:text-slate-400">
                Check back tomorrow for the next pulse question.
              </p>
            </Card>
          )}
        </div>
      </main>
    </>
  );
}
