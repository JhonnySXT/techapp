import { io, Socket } from "socket.io-client";

let socket: Socket | null = null;

export function getSocket() {
  if (socket) {
    return socket;
  }

  socket = io(process.env.NEXT_PUBLIC_SOCKET_URL ?? "http://localhost:4000", {
    transports: ["websocket"],
    withCredentials: true,
  });

  return socket;
}






