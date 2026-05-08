import { cookies } from "next/headers";
import { createServerClient } from "@supabase/auth-helpers-nextjs";
import { getActiveQuestion, getProfile, getUserVote } from "@/lib/queries";
import VoteButtons from "@/components/VoteButtons";
import ResultsBar from "@/components/ResultsBar";
import { Navbar } from "@/components/Navbar";
import { Card } from "@/components/Card";
import { ScrollAnimations } from "@/components/ScrollAnimations";

export default async function HomePage() {
  const supabase = createServerClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL || "",
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || "",
    { cookies: cookies() }
  );
  const {
    data: { session },
  } = await supabase.auth.getSession();

  // If not authenticated, show landing page
  if (!session) {
    return <LandingPage />;
  }

  const userId = session.user.id;
  const profile = await getProfile(supabase, userId);

  if (!profile || !profile.branch || !profile.year || !profile.goal) {
    return <OnboardingRedirect />;
  }

  const activeQuestion = await getActiveQuestion(supabase);
  const vote = activeQuestion ? await getUserVote(supabase, activeQuestion.id, userId) : null;
  const hasVoted = Boolean(vote?.vote || vote?.vote === false);

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
              {profile.branch} · Year {profile.year}
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

function LandingPage() {
  return (
    <>
      <Navbar />
      <ScrollAnimations />
      <main className="min-h-screen bg-gradient-to-br from-white via-blue-50 to-indigo-100 text-slate-900 dark:from-slate-900 dark:via-slate-800 dark:to-blue-900 dark:text-slate-50">
        {/* Hero Section */}
        <section className="relative overflow-hidden px-4 py-20 sm:px-6 lg:px-8">
          <div className="mx-auto max-w-7xl">
            <div className="text-center">
              <div className="fade-in-up-observer">
                <h1 className="text-4xl font-bold tracking-tight sm:text-6xl lg:text-7xl">
                  <span className="block">Welcome to</span>
                  <span className="block bg-gradient-to-r from-indigo-600 to-cyan-600 bg-clip-text text-transparent dark:from-cyan-400 dark:to-indigo-400">
                    PulseVote
                  </span>
                </h1>
                <p className="mx-auto mt-6 max-w-2xl text-lg text-slate-600 dark:text-slate-300">
                  The mobile-first platform for daily opinion polling. Share your thoughts, see what others think, and be part of meaningful conversations.
                </p>
                <div className="mt-10">
                  <a
                    href="/login"
                    className="rounded-lg bg-indigo-600 px-8 py-3 text-base font-semibold text-white shadow-sm transition hover:bg-indigo-700 dark:bg-cyan-600 dark:hover:bg-cyan-700"
                  >
                    Get Started
                  </a>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Features Section */}
        <section className="px-4 py-20 sm:px-6 lg:px-8">
          <div className="mx-auto max-w-7xl">
            <div className="text-center">
              <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
                Why PulseVote?
              </h2>
              <p className="mt-4 text-lg text-slate-600 dark:text-slate-300">
                Discover the power of daily opinion sharing
              </p>
            </div>

            <div className="mt-16 grid gap-8 sm:grid-cols-2 lg:grid-cols-3">
              <div className="fade-in-up-observer">
                <Card className="h-full p-6 text-center">
                  <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-lg bg-indigo-100 dark:bg-slate-700">
                    <svg className="h-6 w-6 text-indigo-600 dark:text-cyan-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                    </svg>
                  </div>
                  <h3 className="mt-4 text-lg font-semibold">Daily Questions</h3>
                  <p className="mt-2 text-slate-600 dark:text-slate-400">
                    Fresh, thought-provoking questions every day to spark meaningful discussions.
                  </p>
                </Card>
              </div>

              <div className="fade-in-up-observer">
                <Card className="h-full p-6 text-center">
                  <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-lg bg-indigo-100 dark:bg-slate-700">
                    <svg className="h-6 w-6 text-indigo-600 dark:text-cyan-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                    </svg>
                  </div>
                  <h3 className="mt-4 text-lg font-semibold">Community Insights</h3>
                  <p className="mt-2 text-slate-600 dark:text-slate-400">
                    See what your peers think and understand diverse perspectives.
                  </p>
                </Card>
              </div>

              <div className="fade-in-up-observer">
                <Card className="h-full p-6 text-center">
                  <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-lg bg-indigo-100 dark:bg-slate-700">
                    <svg className="h-6 w-6 text-indigo-600 dark:text-cyan-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                    </svg>
                  </div>
                  <h3 className="mt-4 text-lg font-semibold">Mobile First</h3>
                  <p className="mt-2 text-slate-600 dark:text-slate-400">
                    Optimized for mobile devices with a seamless voting experience.
                  </p>
                </Card>
              </div>
            </div>
          </div>
        </section>

        {/* How It Works Section */}
        <section className="bg-slate-50 px-4 py-20 dark:bg-slate-800 sm:px-6 lg:px-8">
          <div className="mx-auto max-w-7xl">
            <div className="text-center">
              <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
                How It Works
              </h2>
              <p className="mt-4 text-lg text-slate-600 dark:text-slate-300">
                Simple steps to join the conversation
              </p>
            </div>

            <div className="mt-16">
              <div className="grid gap-8 lg:grid-cols-3">
                <div className="fade-in-up-observer text-center">
                  <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-indigo-600 text-2xl font-bold text-white dark:bg-cyan-600">
                    1
                  </div>
                  <h3 className="mt-6 text-xl font-semibold">Sign Up</h3>
                  <p className="mt-2 text-slate-600 dark:text-slate-400">
                    Create your account with email or Google OAuth and complete your profile.
                  </p>
                </div>

                <div className="fade-in-up-observer text-center">
                  <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-indigo-600 text-2xl font-bold text-white dark:bg-cyan-600">
                    2
                  </div>
                  <h3 className="mt-6 text-xl font-semibold">Vote Daily</h3>
                  <p className="mt-2 text-slate-600 dark:text-slate-400">
                    Answer thought-provoking questions and share your opinions with the community.
                  </p>
                </div>

                <div className="fade-in-up-observer text-center">
                  <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-indigo-600 text-2xl font-bold text-white dark:bg-cyan-600">
                    3
                  </div>
                  <h3 className="mt-6 text-xl font-semibold">See Results</h3>
                  <p className="mt-2 text-slate-600 dark:text-slate-400">
                    View real-time results and discover what others in your community think.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* CTA Section */}
        <section className="px-4 py-20 sm:px-6 lg:px-8">
          <div className="mx-auto max-w-3xl text-center">
            <div className="fade-in-up-observer">
              <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
                Ready to Share Your Voice?
              </h2>
              <p className="mt-4 text-lg text-slate-600 dark:text-slate-300">
                Join thousands of users who are already part of the daily conversation.
              </p>
              <div className="mt-10">
                <a
                  href="/login"
                  className="rounded-lg bg-indigo-600 px-8 py-3 text-base font-semibold text-white shadow-sm transition hover:bg-indigo-700 dark:bg-cyan-600 dark:hover:bg-cyan-700"
                >
                  Start Voting Today
                </a>
              </div>
            </div>
          </div>
        </section>
      </main>
    </>
  );
}

function OnboardingRedirect() {
  return (
    <>
      <Navbar />
      <main className="flex min-h-screen items-center justify-center bg-gradient-to-br from-white via-blue-50 to-indigo-100 px-4 dark:from-slate-900 dark:via-slate-800 dark:to-blue-900">
        <Card className="max-w-md p-8 text-center">
          <h1 className="text-2xl font-bold">Complete Your Profile</h1>
          <p className="mt-4 text-slate-600 dark:text-slate-400">
            Please finish setting up your profile to continue.
          </p>
          <div className="mt-6">
            <a
              href="/onboarding"
              className="inline-block rounded-lg bg-indigo-600 px-6 py-2 text-sm font-semibold text-white transition hover:bg-indigo-700 dark:bg-cyan-600 dark:hover:bg-cyan-700"
            >
              Go to Onboarding
            </a>
          </div>
        </Card>
      </main>
    </>
  );
}
