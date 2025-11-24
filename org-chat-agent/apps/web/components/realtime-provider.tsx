"use client";

import { ReactNode, useEffect } from "react";
import { useRealtimeClient } from "../hooks/useRealtimeClient";

interface Props {
  children: ReactNode;
}

export function RealtimeProvider({ children }: Props) {
  const { connect } = useRealtimeClient();

  useEffect(() => {
    const teardown = connect();
    return () => {
      teardown?.();
    };
  }, [connect]);

  return <>{children}</>;
}






