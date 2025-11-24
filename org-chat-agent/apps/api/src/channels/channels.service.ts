import { Inject, Injectable, Logger, forwardRef } from "@nestjs/common";
import { PrismaService } from "../prisma/prisma.service";
import { SendMessageDto } from "./dto/send-message.dto";
import { ChatGateway } from "../chat/chat.gateway";

interface ChannelResponse {
  id: string;
  name: string;
  description?: string;
  unreadCount: number;
}

interface MessageResponse {
  id: string;
  channelId: string;
  body: string;
  author: string;
  createdAt: string;
}

const randomId = () => Math.random().toString(36).slice(2);

@Injectable()
export class ChannelsService {
  private readonly logger = new Logger(ChannelsService.name);
  private readonly memoryMessages = new Map<string, MessageResponse[]>();
  private readonly fallbackChannels: ChannelResponse[] = [
    { id: "general", name: "Генеральный", description: "Основные объявления", unreadCount: 2 },
    { id: "support", name: "Служба поддержки", description: "Запросы клиентов", unreadCount: 0 },
    { id: "rnd", name: "R&D", description: "Идеи и эксперименты", unreadCount: 5 },
  ];

  constructor(
    private readonly prisma: PrismaService,
    @Inject(forwardRef(() => ChatGateway))
    private readonly chatGateway: ChatGateway,
  ) {}

  private remember(message: MessageResponse) {
    const list = this.memoryMessages.get(message.channelId) ?? [];
    list.push(message);
    this.memoryMessages.set(message.channelId, list);
  }

  async listChannels(): Promise<{ channels: ChannelResponse[] }> {
    try {
      const channels = await this.prisma.channel.findMany({
        include: {
          _count: {
            select: { messages: true },
          },
        },
      });

      if (channels.length) {
        return {
          channels: channels.map((channel) => ({
            id: channel.id,
            name: channel.name,
            description: channel.description ?? undefined,
            unreadCount: channel._count.messages,
          })),
        };
      }
    } catch (error) {
      this.logger.warn(`Fallback channels used: ${(error as Error).message}`);
    }

    return {
      channels: this.fallbackChannels.map((channel) => ({
        ...channel,
        unreadCount: this.memoryMessages.get(channel.id)?.length ?? channel.unreadCount,
      })),
    };
  }

  async listMessages(channelId: string) {
    try {
      const messages = await this.prisma.message.findMany({
        where: { channelId },
        orderBy: { createdAt: "asc" },
        select: {
          id: true,
          body: true,
          createdAt: true,
          channelId: true,
          author: { select: { name: true } },
        },
      });

      if (messages.length) {
        return {
          messages: messages.map((message) => ({
            id: message.id,
            body: message.body,
            createdAt: message.createdAt.toISOString(),
            channelId: message.channelId,
            author: message.author?.name ?? "Неизвестно",
          })),
        };
      }
    } catch (error) {
      this.logger.warn(`Fallback messages used: ${(error as Error).message}`);
    }

    return {
      messages: this.memoryMessages.get(channelId) ?? [],
    };
  }

  async createMessage(channelId: string, dto: SendMessageDto) {
    const timestamp = new Date();
    const authorName = dto.author ?? "Сотрудник";

    try {
      const message = await this.prisma.message.create({
        data: {
          body: dto.body,
          channelId,
          author: {
            connectOrCreate: {
              where: { email: "system@org.chat" },
              create: {
                email: "system@org.chat",
                name: authorName,
                password: "temporary",
              },
            },
          },
        },
        include: {
          author: true,
        },
      });

      const payload: MessageResponse = {
        id: message.id,
        channelId,
        body: message.body,
        author: message.author?.name ?? authorName,
        createdAt: message.createdAt.toISOString(),
      };

      this.chatGateway.broadcast(payload);
      return payload;
    } catch (error) {
      this.logger.warn(`Message stored in memory: ${(error as Error).message}`);

      const fallback: MessageResponse = {
        id: randomId(),
        channelId,
        body: dto.body,
        author: authorName,
        createdAt: timestamp.toISOString(),
      };

      this.remember(fallback);
      this.chatGateway.broadcast(fallback);
      return fallback;
    }
  }
}






