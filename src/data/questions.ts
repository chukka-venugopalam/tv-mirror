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
    isActive: true,
  },
  {
    id: "3",
    text: "Should social media platforms be regulated more strictly?",
    isActive: true,
  },
  {
    id: "4",
    text: "Do you support universal basic income?",
    isActive: true,
  },
  {
    id: "5",
    text: "Should college education be free for all citizens?",
    isActive: true,
  },
];