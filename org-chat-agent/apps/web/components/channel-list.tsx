"use client";

import { useEffect } from "react";
import { Button } from "@org-chat/ui";
import { useChannelStore } from "../store/channel-store";

export function ChannelList() {
  const channels = useChannelStore((state) => state.channels);
  const selected = useChannelStore((state) => state.selectedChannelId);
  const selectChannel = useChannelStore((state) => state.selectChannel);
  const bootstrap = useChannelStore((state) => state.bootstrap);

  useEffect(() => {
    void bootstrap();
  }, [bootstrap]);

  return (
    <aside className="flex h-full flex-col border-r border-white/5 bg-slate-900/40 p-4 backdrop-blur">
      <div className="mb-4 flex items-center justify-between">
        <div>
          <p className="text-xs uppercase text-slate-400">Оргчат</p>
          <p className="text-lg font-semibold">Каналы</p>
        </div>
        <Button variant="ghost" className="text-xs text-slate-400">
          + Канал
        </Button>
      </div>

      <ul className="space-y-1 overflow-y-auto">
        {channels.map((channel) => (
          <li key={channel.id}>
            <button
              onClick={() => selectChannel(channel.id)}
              className={`flex w-full items-center justify-between rounded-md px-3 py-2 text-left text-sm transition ${
                selected === channel.id
                  ? "bg-white/10 text-white"
                  : "text-slate-300 hover:bg-white/5"
              }`}
            >
              <span>{channel.name}</span>
              {channel.unreadCount > 0 && (
                <span className="rounded-full bg-indigo-600 px-2 py-0.5 text-xs">
                  {channel.unreadCount}
                </span>
              )}
            </button>
          </li>
        ))}
      </ul>
    </aside>
  );
}






