import { PrismaClient, UserRole, ChannelRole } from "@prisma/client";

const prisma = new PrismaClient();

const channelsSeed = [
  {
    slug: "general",
    name: "Генеральный",
    description: "Главные объявления и апдейты по компании",
  },
  {
    slug: "support",
    name: "Служба поддержки",
    description: "Кейсы клиентов, эскалации и ответы",
  },
  {
    slug: "rnd",
    name: "R&D",
    description: "Идеи, прототипы и эксперименты",
  },
];

async function main() {
  const admin = await prisma.user.upsert({
    where: { email: "system@org.chat" },
    update: {
      name: "Комс Старший",
      role: UserRole.ADMIN,
    },
    create: {
      email: "system@org.chat",
      name: "Комс Старший",
      password: "replace-with-hash",
      role: UserRole.ADMIN,
    },
  });

  const channelsMap = new Map<string, string>();

  for (const channel of channelsSeed) {
    const record = await prisma.channel.upsert({
      where: { name: channel.name },
      update: {
        description: channel.description,
      },
      create: {
        name: channel.name,
        description: channel.description,
      },
    });

    channelsMap.set(channel.slug, record.id);

    await prisma.membership.upsert({
      where: {
        userId_channelId: {
          userId: admin.id,
          channelId: record.id,
        },
      },
      update: {},
      create: {
        userId: admin.id,
        channelId: record.id,
        role: ChannelRole.OWNER,
      },
    });
  }

  const generalId = channelsMap.get("general");
  const supportId = channelsMap.get("support");

  if (generalId) {
    const count = await prisma.message.count({ where: { channelId: generalId } });
    if (count === 0) {
      await prisma.message.create({
        data: {
          body: "Запустили каркас монорепозитория. Следующий шаг — auth и миграции.",
          channelId: generalId,
          authorId: admin.id,
        },
      });
    }
  }

  if (supportId) {
    const count = await prisma.message.count({ where: { channelId: supportId } });
    if (count === 0) {
      await prisma.message.create({
        data: {
          body: "Тут будем собирать обращения внутренних команд и клиентов.",
          channelId: supportId,
          authorId: admin.id,
        },
      });
    }
  }
}

main()
  .then(async () => {
    await prisma.$disconnect();
  })
  .catch(async (error) => {
    console.error("Seed failed", error);
    await prisma.$disconnect();
    process.exit(1);
  });






