export interface Channel {
  id: string;
  name: string;
  description?: string;
  unreadCount: number;
}

export interface Message {
  id: string;
  channelId: string;
  author: string;
  body: string;
  createdAt: string;
  optimistic?: boolean;
}






