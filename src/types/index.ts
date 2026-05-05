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
        Insert: {
          id: string;
          branch?: string | null;
          year?: number | null;
          goal?: string | null;
          role?: string;
        };
        Update: {
          branch?: string | null;
          year?: number | null;
          goal?: string | null;
          role?: string;
        };
      };
      questions: {
        Row: Question;
        Insert: {
          id?: string;
          text: string;
          is_active?: boolean;
        };
        Update: {
          text?: string;
          is_active?: boolean;
        };
      };
      votes: {
        Row: Vote;
        Insert: {
          user_id: string;
          question_id: string;
          vote: boolean;
        };
        Update: {
          vote?: boolean;
        };
      };
    };
  };
};
