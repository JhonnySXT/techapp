import { Injectable } from "@nestjs/common";

@Injectable()
export class AppService {
  getStatus() {
    return {
      ok: true,
      service: "org-chat-api",
      timestamp: new Date().toISOString(),
    };
  }
}






