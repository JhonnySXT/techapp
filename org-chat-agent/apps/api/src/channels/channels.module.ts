import { Module, forwardRef } from "@nestjs/common";
import { ChannelsController } from "./channels.controller";
import { ChannelsService } from "./channels.service";
import { PrismaModule } from "../prisma/prisma.module";
import { ChatModule } from "../chat/chat.module";

@Module({
  imports: [PrismaModule, forwardRef(() => ChatModule)],
  controllers: [ChannelsController],
  providers: [ChannelsService],
  exports: [ChannelsService],
})
export class ChannelsModule {}






