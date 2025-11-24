"use client";

import { FormEvent, useMemo, useRef } from "react";
import { Button } from "@org-chat/ui";
import { useChannelStore } from "../store/channel-store";

export function ChatWindow() {
  const selectedChannelId = useChannelStore((state) => state.selectedChannelId);
  const channels = useChannelStore((state) => state.channels);
  const messages = useChannelStore((state) => state.messages);
  const sendMessage = useChannelStore((state) => state.sendMessage);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const channel = useMemo(
    () => channels.find((ch) => ch.id === selectedChannelId),
    [channels, selectedChannelId],
  );

  const channelMessages = selectedChannelId ? messages[selectedChannelId] ?? [] : [];

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const value = textareaRef.current?.value ?? "";
    if (!value.trim()) {
      return;
    }
    await sendMessage(value);
    if (textareaRef.current) {
      textareaRef.current.value = "";
    }
  };

  if (!channel) {
    return (
      <section className="flex h-full flex-1 items-center justify-center text-slate-400">
        Выберите канал, чтобы увидеть историю сообщений.
      </section>
    );
  }

  return (
    <section className="flex h-full flex-1 flex-col">
      <header className="border-b border-white/5 px-6 py-4">
        <p className="text-lg font-semibold">{channel.name}</p>
        {channel.description && <p className="text-sm text-slate-400">{channel.description}</p>}
      </header>

      <div className="flex-1 space-y-4 overflow-y-auto px-6 py-4">
        {channelMessages.map((message) => (
          <article key={message.id} className="rounded-lg bg-white/5 p-3 text-sm">
            <div className="flex items-center justify-between text-xs text-slate-400">
              <span>{message.author}</span>
              <span>
                {new Date(message.createdAt).toLocaleTimeString("ru-RU", {
                  hour: "2-digit",
                  minute: "2-digit",
                })}
              </span>
            </div>
            <p className="mt-1 whitespace-pre-line text-slate-100">{message.body}</p>
            {message.optimistic && (
              <p className="mt-1 text-xs text-amber-400">Отправляем через API…</p>
            )}
          </article>
        ))}
      </div>

      <form onSubmit={handleSubmit} className="border-t border-white/5 px-6 py-4">
        <textarea
          ref={textareaRef}
          rows={3}
          placeholder="Введите сообщение…"
          className="w-full rounded-md bg-white/5 px-3 py-2 text-sm text-white outline-none ring-1 ring-white/10 focus:ring-indigo-500"
        />
        <div className="mt-3 flex justify-end">
          <Button type="submit">Отправить</Button>
        </div>
      </form>
    </section>
  );
}






