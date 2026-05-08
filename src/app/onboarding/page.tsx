"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { createBrowserClient } from "@supabase/auth-helpers-nextjs";
import type { Database } from "@/types";
import { Navbar } from "@/components/Navbar";
import { Card } from "@/components/Card";

export default function OnboardingPage() {
  const supabase = createBrowserClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL || "",
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || ""
  ) as any;
  const router = useRouter();
  const [branch, setBranch] = useState("");
  const [year, setYear] = useState("");
  const [goal, setGoal] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadProfile() {
      const {
        data: { session },
      } = await supabase.auth.getSession();
      if (!session) {
        router.replace("/login");
        return;
      }
      const { data: profile } = await supabase.from("profiles").select("branch, year, goal").eq("id", session.user.id).single();
      if (profile?.branch && profile?.year && profile?.goal) {
        router.replace("/");
        return;
      }
      setLoading(false);
    }

    loadProfile();
  }, [router, supabase]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    if (!branch.trim() || !year.trim() || !goal.trim()) {
      setError("Please complete all fields.");
      setSubmitting(false);
      return;
    }

    const numericYear = Number(year);
    if (Number.isNaN(numericYear) || numericYear <= 0) {
      setError("Enter a valid year.");
      setSubmitting(false);
      return;
    }

    const response = await fetch("/api/profile", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ branch: branch.trim(), year: numericYear, goal: goal.trim() }),
    });

    if (!response.ok) {
      const data = await response.json();
      setError(data?.error || "Unable to save onboarding data.");
      setSubmitting(false);
      return;
    }

    router.push("/");
  }

  return (
    <>
      <Navbar />
      <main className="min-h-screen bg-gradient-to-br from-white via-blue-50 to-indigo-100 px-4 py-12 dark:from-slate-900 dark:via-slate-800 dark:to-blue-900">
        <div className="mx-auto max-w-md">
          <Card className="p-8">
            <div className="space-y-8">
              <div className="space-y-2 text-center">
                <p className="text-sm font-bold uppercase tracking-wider text-indigo-600 dark:text-cyan-400">
                  Welcome to PulseVote
                </p>
                <h1 className="text-3xl font-bold">Complete your profile</h1>
                <p className="text-sm text-slate-600 dark:text-slate-400">
                  This helps customize your experience.
                </p>
              </div>

              {loading ? (
                <div className="rounded-lg bg-slate-100 p-6 text-center text-slate-600 dark:bg-slate-800 dark:text-slate-400">
                  Loading…
                </div>
              ) : (
                <form onSubmit={handleSubmit} className="space-y-4">
                  <div className="space-y-2">
                    <label className="block text-sm font-medium text-slate-700 dark:text-slate-300">
                      Branch
                    </label>
                    <input
                      value={branch}
                      onChange={(event) => setBranch(event.target.value)}
                      placeholder="e.g. Engineering"
                      className="w-full rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm text-slate-900 outline-none transition focus:border-indigo-500 dark:border-slate-700 dark:bg-slate-700 dark:text-slate-50 dark:focus:border-cyan-400"
                    />
                  </div>
                  <div className="space-y-2">
                    <label className="block text-sm font-medium text-slate-700 dark:text-slate-300">
                      Year
                    </label>
                    <input
                      value={year}
                      onChange={(event) => setYear(event.target.value)}
                      placeholder="e.g. 2026"
                      inputMode="numeric"
                      className="w-full rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm text-slate-900 outline-none transition focus:border-indigo-500 dark:border-slate-700 dark:bg-slate-700 dark:text-slate-50 dark:focus:border-cyan-400"
                    />
                  </div>
                  <div className="space-y-2">
                    <label className="block text-sm font-medium text-slate-700 dark:text-slate-300">
                      Goal
                    </label>
                    <textarea
                      value={goal}
                      onChange={(event) => setGoal(event.target.value)}
                      placeholder="What do you want to achieve this year?"
                      rows={3}
                      className="w-full rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm text-slate-900 outline-none transition focus:border-indigo-500 dark:border-slate-700 dark:bg-slate-700 dark:text-slate-50 dark:focus:border-cyan-400"
                    />
                  </div>
                  {error && <p className="text-sm text-red-600 dark:text-red-400">{error}</p>}
                  <button
                    type="submit"
                    disabled={submitting}
                    className="w-full rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-700 disabled:opacity-60 dark:bg-cyan-600 dark:hover:bg-cyan-700"
                  >
                    Save profile
                  </button>
                </form>
              )}
            </div>
          </Card>
        </div>
      </main>
    </>
  );
}
