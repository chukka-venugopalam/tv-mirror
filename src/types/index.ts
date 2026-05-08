export type Json = string | number | boolean | null | { [key: string]: Json } | Json[];

export type Profile = {
  id: string;
  branch: string | null;
  year: number | null;
  goal: string | null;
  role: string;
  created_at: string;
};

export type Question = {
  id: string;
  text: string;
  is_active: boolean;
  created_at: string;
};

export type Vote = {
  id: string;
  user_id: string;
  question_id: string;
  vote: boolean;
  created_at: string;
};

export type Database = {
  public: {
    Tables: {
      profiles: {
        Row: Profile;
        Insert: Record<string, any>;
        Update: Record<string, any>;
      };
      questions: {
        Row: Question;
        Insert: Record<string, any>;
        Update: Record<string, any>;
      };
      votes: {
        Row: Vote;
        Insert: Record<string, any>;
        Update: Record<string, any>;
      };
    };
    Views: Record<string, any>;
    Functions: Record<string, any>;
    Enums: Record<string, any>;
  };
};
