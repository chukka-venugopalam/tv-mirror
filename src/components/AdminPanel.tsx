"use client";

import { useEffect, useState } from "react";
import type { Question } from "@/types";

type AdminStats = {
  totalUsers: number;
  totalVotes: number;
};

type AdminQuestion = Question & {
  voteCount: number;
  agreeCount: number;
  disagreeCount: number;
};

export default function AdminPanel() {
  const [questions, setQuestions] = useState<AdminQuestion[]>([]);
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [text, setText] = useState("");
  const [isActive, setIsActive] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch("/api/admin/questions");
      if (!response.ok) {
        const data = await response.json();
        throw new Error(data?.error || "Failed to load admin data");
      }
      const data = await response.json();
      setQuestions(data.questions || []);
      setStats({ totalUsers: data.totalUsers, totalVotes: data.totalVotes });
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function createQuestion() {
    if (!text.trim()) {
      setError("Question text is required.");
      return;
    }
    setError(null);
    const response = await fetch("/api/admin/questions", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ text: text.trim(), isActive }),
    });
    if (!response.ok) {
      const data = await response.json();
      setError(data?.error || "Unable to create question.");
      return;
    }
    setText("");
    setIsActive(true);
    await load();
  }

  async function updateQuestion(id: string) {
    const newText = window.prompt("Update question text:", questions.find((item) => item.id === id)?.text || "");
    if (!newText || !newText.trim()) {
      return;
    }
    const response = await fetch("/api/admin/questions", {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ id, action: "update", text: newText.trim() }),
    });
    if (response.ok) {
      await load();
    }
  }

  async function toggleActive(id: string) {
    const response = await fetch("/api/admin/questions", {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ id, action: "toggle" }),
    });
    if (response.ok) {
      await load();
    }
  }

  async function deleteQuestion(id: string) {
    if (!window.confirm("Delete this question?")) {
      return;
    }
    const response = await fetch(`/api/admin/questions?id=${id}`, {
      method: "DELETE",
    });
    if (response.ok) {
      await load();
    }
  }

  return (
    <div className="space-y-8">
      <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm uppercase tracking-[0.24em] text-sky-600">Admin panel</p>
            <h2 className="mt-2 text-3xl font-semibold text-slate-950">Manage questions</h2>
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <div className="rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-700">
              Total users
              <div className="mt-2 text-2xl font-semibold text-slate-900">{stats?.totalUsers ?? "—"}</div>
            </div>
            <div className="rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-700">
              Total votes
              <div className="mt-2 text-2xl font-semibold text-slate-900">{stats?.totalVotes ?? "—"}</div>
            </div>
          </div>
        </div>
      </div>

      <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
        <div className="space-y-4">
          <label className="block text-sm font-medium text-slate-700">New question</label>
          <textarea
            rows={3}
            value={text}
            onChange={(event) => setText(event.target.value)}
            className="w-full rounded-3xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-sky-500"
            placeholder="Enter a question for the next daily vote"
          />
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <label className="flex items-center gap-2 text-sm text-slate-700">
              <input
                type="checkbox"
                checked={isActive}
                onChange={(event) => setIsActive(event.target.checked)}
                className="h-4 w-4 rounded border-slate-300 text-sky-600"
              />
              Set as active question
            </label>
            <button
              type="button"
              onClick={createQuestion}
              className="rounded-2xl bg-sky-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-sky-700"
            >
              Save question
            </button>
          </div>
          {error ? <p className="text-sm text-red-600">{error}</p> : null}
        </div>
      </div>

      <div className="space-y-4">
        {loading ? (
          <div className="rounded-3xl border border-slate-200 bg-white p-6 text-slate-600">Loading questions…</div>
        ) : questions.length === 0 ? (
          <div className="rounded-3xl border border-slate-200 bg-white p-6 text-slate-600">No questions available.</div>
        ) : (
          questions.map((question) => {
            const agreePercent = question.voteCount ? Math.round((question.agreeCount / question.voteCount) * 100) : 0;
            const disagreePercent = question.voteCount ? Math.round((question.disagreeCount / question.voteCount) * 100) : 0;

            return (
              <div key={question.id} className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
                <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p className="text-sm text-slate-500">Question</p>
                    <p className="mt-2 text-lg font-semibold text-slate-900">{question.text}</p>
                    <div className="mt-4 grid gap-3 sm:grid-cols-3">
                      <div className="rounded-2xl bg-slate-50 px-3 py-2 text-sm text-slate-700">
                        Total votes
                        <div className="mt-1 text-xl font-semibold text-slate-900">{question.voteCount}</div>
                      </div>
                      <div className="rounded-2xl bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
                        Agree
                        <div className="mt-1 text-xl font-semibold">{agreePercent}%</div>
                      </div>
                      <div className="rounded-2xl bg-rose-50 px-3 py-2 text-sm text-rose-700">
                        Disagree
                        <div className="mt-1 text-xl font-semibold">{disagreePercent}%</div>
                      </div>
                    </div>
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    <span className={`rounded-full px-3 py-1 text-sm ${question.is_active ? "bg-emerald-100 text-emerald-700" : "bg-slate-100 text-slate-600"}`}>
                      {question.is_active ? "Active" : "Inactive"}
                    </span>
                    <button
                      className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-2 text-sm text-slate-900 transition hover:border-slate-300"
                      onClick={() => updateQuestion(question.id)}
                    >
                      Edit
                    </button>
                    <button
                      className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-2 text-sm text-slate-900 transition hover:border-slate-300"
                      onClick={() => toggleActive(question.id)}
                    >
                      Set active
                    </button>
                    <button
                      className="rounded-2xl border border-red-200 bg-red-50 px-4 py-2 text-sm text-red-700 transition hover:bg-red-100"
                      onClick={() => deleteQuestion(question.id)}
                    >
                      Delete
                    </button>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
