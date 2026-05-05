"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

type VoteButtonsProps = {
  questionId: string;
  questionText: string;
};

export default function VoteButtons({ questionId, questionText }: VoteButtonsProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();

  async function castVote(value: boolean) {
    setLoading(true);
    setError(null);

    const response = await fetch("/api/vote", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ questionId, vote: value, questionText }),
    });

    if (!response.ok) {
      const result = await response.json();
      setError(result?.error || "Unable to submit your vote.");
      setLoading(false);
      return;
    }

    router.refresh();
  }

  return (
    <div className="space-y-4 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
      <p className="text-sm uppercase tracking-[0.24em] text-slate-500">Your response</p>
      {error ? <p className="text-sm text-red-600">{error}</p> : null}
      <div className="grid gap-3 sm:grid-cols-2">
        <button
          className="rounded-2xl bg-sky-600 px-5 py-4 text-sm font-semibold text-white transition hover:bg-sky-700 disabled:cursor-not-allowed disabled:opacity-60"
          onClick={() => castVote(true)}
          disabled={loading}
        >
          Agree
        </button>
        <button
          className="rounded-2xl border border-slate-200 bg-white px-5 py-4 text-sm font-semibold text-slate-900 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
          onClick={() => castVote(false)}
          disabled={loading}
        >
          Disagree
        </button>
      </div>
    </div>
  );
}
