import { IsOptional, IsString, Length } from "class-validator";

export class SendMessageDto {
  @IsString()
  @Length(1, 2000)
  body!: string;

  @IsOptional()
  @IsString()
  author?: string;
}






