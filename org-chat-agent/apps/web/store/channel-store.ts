"use client";

import { create } from "zustand";
import { Channel, Message } from "../types/chat";
import { apiClient } from "../lib/api-client";

const fallbackChannels: Channel[] = [
  { id: "general", name: "Генеральный", description: "Основные объявления", unreadCount: 2 },
  { id: "support", name: "Служба поддержки", description: "Запросы клиентов", unreadCount: 0 },
  { id: "rnd", name: "R&D", description: "Идеи и эксперименты", unreadCount: 5 },
];

const fallbackMessages: Record<string, Message[]> = {
  general: [
    {
      id: "m1",
      channelId: "general",
      author: "Игорь",
      body: "Запустили каркас монорепозитория. Готовимся к первому релизу.",
      createdAt: new Date().toISOString(),
    },
  ],
};

const randomId = () => Math.random().toString(36).slice(2);

export interface ChannelState {
  channels: Channel[];
  selectedChannelId?: string;
  messages: Record<string, Message[]>;
  bootstrap: () => Promise<void>;
  selectChannel: (channelId: string) => void;
  pushMessage: (message: Message) => void;
  sendMessage: (body: string) => Promise<void>;
}

export const useChannelStore = create<ChannelState>((set, get) => ({
  channels: [],
  selectedChannelId: undefined,
  messages: {},
  bootstrap: async () => {
    if (get().channels.length > 0) {
      return;
    }

    try {
      const { data } = await apiClient.get<{ channels: Channel[] }>("/channels");
      const channels = data.channels ?? fallbackChannels;
      const firstChannelId = channels[0]?.id;

      set({
        channels,
        selectedChannelId: firstChannelId,
      });

      if (firstChannelId) {
        const { data: messagesResponse } = await apiClient.get<{ messages: Message[] }>(
          `/channels/${firstChannelId}/messages`,
        );

        set((state) => ({
          messages: {
            ...state.messages,
            [firstChannelId]: messagesResponse.messages ?? [],
          },
        }));
      }
    } catch {
      set({
        channels: fallbackChannels,
        selectedChannelId: fallbackChannels[0]?.id,
        messages: fallbackMessages,
      });
    }
  },
  selectChannel: (channelId) => set({ selectedChannelId: channelId }),
  pushMessage: (message) =>
    set((state) => {
      const existing = state.messages[message.channelId] ?? [];
      return {
        messages: {
          ...state.messages,
          [message.channelId]: [...existing, message],
        },
      };
    }),
  sendMessage: async (body: string) => {
    const channelId = get().selectedChannelId;
    if (!channelId || !body.trim()) {
      return;
    }

    const optimisticMessage: Message = {
      id: randomId(),
      channelId,
      author: "Вы",
      body,
      createdAt: new Date().toISOString(),
      optimistic: true,
    };

    set((state) => ({
      messages: {
        ...state.messages,
        [channelId]: [...(state.messages[channelId] ?? []), optimisticMessage],
      },
    }));

    try {
      const { data } = await apiClient.post<Message>(`/channels/${channelId}/messages`, { body });
      set((state) => ({
        messages: {
          ...state.messages,
          [channelId]: state.messages[channelId]?.map((msg) =>
            msg.id === optimisticMessage.id ? data : msg,
          ),
        },
      }));
    } catch {
      set((state) => ({
        messages: {
          ...state.messages,
          [channelId]: state.messages[channelId]?.map((msg) =>
            msg.id === optimisticMessage.id
              ? { ...msg, optimistic: false, body: `${msg.body} (не доставлено)` }
              : msg,
          ),
        },
      }));
    }
  },
}));

