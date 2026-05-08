import { redirect } from "next/navigation";
import { cookies } from "next/headers";
import { createServerClient } from "@supabase/auth-helpers-nextjs";
import AdminPanel from "@/components/AdminPanel";
import { Navbar } from "@/components/Navbar";

export default async function AdminPage() {
  const supabase = createServerClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL || "",
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || "",
    { cookies: cookies() }
  );

  const {
    data: { session },
  } = await supabase.auth.getSession();

  if (!session) {
    redirect("/login");
  }

  // Check if user is admin
  const adminEmail = process.env.NEXT_PUBLIC_ADMIN_EMAIL;
  if (session.user.email !== adminEmail) {
    redirect("/");
  }

  return (
    <>
      <Navbar />
      <main className="min-h-screen bg-gradient-to-br from-white via-blue-50 to-indigo-100 px-4 py-12 dark:from-slate-900 dark:via-slate-800 dark:to-blue-900">
        <div className="mx-auto max-w-5xl">
          <AdminPanel />
        </div>
      </main>
    </>
  );
}
