import { Module, forwardRef } from "@nestjs/common";
import { ChatGateway } from "./chat.gateway";
import { ChannelsModule } from "../channels/channels.module";

@Module({
  imports: [forwardRef(() => ChannelsModule)],
  providers: [ChatGateway],
  exports: [ChatGateway],
})
export class ChatModule {}






