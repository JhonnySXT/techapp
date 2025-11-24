import { Injectable } from "@nestjs/common";

@Injectable()
export class HealthService {
  check() {
    return {
      ok: true,
      db: "pending",
      redis: "pending",
      timestamp: new Date().toISOString(),
    };
  }
}






