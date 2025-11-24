import { Body, Controller, Get, Param, Post } from "@nestjs/common";
import { ChannelsService } from "./channels.service";
import { SendMessageDto } from "./dto/send-message.dto";

@Controller("channels")
export class ChannelsController {
  constructor(private readonly channelsService: ChannelsService) {}

  @Get()
  findAll() {
    return this.channelsService.listChannels();
  }

  @Get(":id/messages")
  async messages(@Param("id") id: string) {
    return this.channelsService.listMessages(id);
  }

  @Post(":id/messages")
  async sendMessage(@Param("id") id: string, @Body() dto: SendMessageDto) {
    return this.channelsService.createMessage(id, dto);
  }
}






