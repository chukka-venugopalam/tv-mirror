"use client";

import { useEffect, useState } from "react";

type ResultsBarProps = {
  questionId: string;
};

type ResultsData = {
  agree: number;
  disagree: number;
  total: number;
};

export default function ResultsBar({ questionId }: ResultsBarProps) {
  const [results, setResults] = useState<ResultsData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function fetchResults() {
      setLoading(true);
      setError(null);
      try {
        const res = await fetch(`/api/results?questionId=${questionId}`);
        if (!res.ok) {
          const data = await res.json();
          throw new Error(data?.error || "Failed to load results");
        }
        const data = await res.json();
        setResults(data);
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    }

    fetchResults();
  }, [questionId]);

  if (loading) {
    return (
      <div className="rounded-3xl border border-slate-200 bg-white p-6 text-slate-600">
        Loading results...
      </div>
    );
  }

  if (error || !results) {
    return (
      <div className="rounded-3xl border border-slate-200 bg-white p-6 text-red-600">
        {error || "Unable to load vote results."}
      </div>
    );
  }

  const agreePercent = results.total ? Math.round((results.agree / results.total) * 100) : 0;
  const disagreePercent = results.total ? 100 - agreePercent : 0;

  return (
    <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex items-center justify-between gap-4">
        <div>
          <p className="text-sm uppercase tracking-[0.24em] text-slate-500">Results</p>
          <p className="mt-2 text-2xl font-semibold text-slate-900">Voted: {results.total}</p>
        </div>
      </div>
      <div className="mt-6 space-y-4">
        <div className="space-y-2">
          <div className="flex items-center justify-between text-sm text-slate-700">
            <span>Agree</span>
            <span>{agreePercent}%</span>
          </div>
          <div className="h-3 overflow-hidden rounded-full bg-slate-100">
            <div className="h-full rounded-full bg-sky-600" style={{ width: `${agreePercent}%` }} />
          </div>
        </div>
        <div className="space-y-2">
          <div className="flex items-center justify-between text-sm text-slate-700">
            <span>Disagree</span>
            <span>{disagreePercent}%</span>
          </div>
          <div className="h-3 overflow-hidden rounded-full bg-slate-100">
            <div className="h-full rounded-full bg-cyan-900" style={{ width: `${disagreePercent}%` }} />
          </div>
        </div>
      </div>
    </div>
  );
}
