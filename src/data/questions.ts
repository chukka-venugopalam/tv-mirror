export interface Question {
  id: string;
  text: string;
  isActive: boolean;
}

export const questions: Question[] = [
  {
    id: "1",
    text: "Should remote work be the default for most office jobs?",
    isActive: true,
  },
  {
    id: "2",
    text: "Is artificial intelligence more beneficial than harmful to society?",
    isActive: false,
  },
  {
    id: "3",
    text: "Should social media platforms be regulated more strictly?",
    isActive: false,
  },
];