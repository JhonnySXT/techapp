import {
  ConnectedSocket,
  MessageBody,
  OnGatewayInit,
  SubscribeMessage,
  WebSocketGateway,
  WebSocketServer,
} from "@nestjs/websockets";
import { Inject, Logger, forwardRef } from "@nestjs/common";
import { Server, Socket } from "socket.io";
import { ChannelsService } from "../channels/channels.service";

@WebSocketGateway({
  cors: {
    origin: (process.env.CORS_ORIGIN ?? "").split(",").filter(Boolean) || true,
    credentials: true,
  },
})
export class ChatGateway implements OnGatewayInit {
  @WebSocketServer()
  server!: Server;

  private readonly logger = new Logger(ChatGateway.name);

  constructor(
    @Inject(forwardRef(() => ChannelsService))
    private readonly channelsService: ChannelsService,
  ) {}

  afterInit() {
    this.logger.log("Realtime gateway ready");
  }

  @SubscribeMessage("channels:join-all")
  handleJoinAll(@ConnectedSocket() client: Socket) {
    client.join("channels:all");
    return { status: "joined" };
  }

  @SubscribeMessage("message:send")
  async handleSendMessage(
    @ConnectedSocket() client: Socket,
    @MessageBody()
    payload: {
      channelId: string;
      body: string;
      author?: string;
    },
  ) {
    const message = await this.channelsService.createMessage(payload.channelId, {
      body: payload.body,
      author: payload.author,
    });

    client.emit("message:ack", message);
    return message;
  }

  broadcast(message: unknown) {
    if (!this.server) {
      this.logger.warn("Gateway not ready yet, skipping broadcast");
      return;
    }
    this.server.to("channels:all").emit("message:new", message);
  }
}

