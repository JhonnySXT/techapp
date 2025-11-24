import { ChannelList } from "../components/channel-list";
import { ChatWindow } from "../components/chat-window";

export default function HomePage() {
  return (
    <div className="mx-auto flex w-full max-w-6xl flex-1 flex-col p-4">
      <main className="flex h-[calc(100vh-120px)] flex-col overflow-hidden rounded-2xl border border-white/5 bg-slate-900/40 backdrop-blur lg:flex-row">
        <div className="w-full border-b border-white/5 lg:w-72 lg:border-b-0 lg:border-r">
          <ChannelList />
        </div>

        <div className="flex w-full flex-1">
          <ChatWindow />
        </div>
      </main>
    </div>
  );
}

