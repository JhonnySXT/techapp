"use client";

import { ThemeProvider } from "next-themes";
import { ReactNode } from "react";
import { RealtimeProvider } from "../components/realtime-provider";

interface Props {
  children: ReactNode;
}

export function Providers({ children }: Props) {
  return (
    <ThemeProvider attribute="class" defaultTheme="dark" enableSystem={false}>
      <RealtimeProvider>{children}</RealtimeProvider>
    </ThemeProvider>
  );
}






