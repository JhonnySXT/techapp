"use client";

import { useCallback } from "react";
import { useChannelStore } from "../store/channel-store";
import { getSocket } from "../lib/socket";
import { Message } from "../types/chat";

export function useRealtimeClient() {
  const bootstrap = useChannelStore((state) => state.bootstrap);
  const pushMessage = useChannelStore((state) => state.pushMessage);

  const connect = useCallback(() => {
    let active = true;

    void bootstrap();
    const socket = getSocket();

    const handleIncoming = (payload: Message) => {
      if (!active) {
        return;
      }
      pushMessage(payload);
    };

    socket.on("message:new", handleIncoming);
    socket.emit("channels:join-all");

    return () => {
      active = false;
      socket.off("message:new", handleIncoming);
    };
  }, [bootstrap, pushMessage]);

  return { connect };
}






