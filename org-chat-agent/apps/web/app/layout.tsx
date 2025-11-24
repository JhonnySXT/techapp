import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { Providers } from "./providers";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "OrgChat • внутренняя связь",
  description: "Командный чат организации с realtime и каналами.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ru" suppressHydrationWarning>
      <body className={`${inter.className} min-h-screen bg-slate-950 text-white`}>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}

