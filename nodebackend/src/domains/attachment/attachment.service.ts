import { Injectable, NotFoundException, StreamableFile } from '@nestjs/common';
import { PrismaService } from '../../prisma.service';
import { createReadStream } from 'fs';
import { join } from 'path';
import * as fs from 'fs';

@Injectable()
export class AttachmentService {
  constructor(private readonly prisma: PrismaService) {}

  private mapAttachment(att: any) {
    return {
      id: att.id,
      itemId: att.item_id,
      commentId: att.comment_id,
      uploaderId: att.uploader_id,
      fileName: att.file_name,
      fileSize: att.file_size.toString(),
      mimeType: att.mime_type,
      storageUrl: att.storage_url,
      createdAt: att.created_at,
    };
  }

  async create(file: Express.Multer.File, itemId: string, userId: string) {
    if (!file) {
      throw new NotFoundException('File is required');
    }

    // Convert size to BigInt for Prisma
    const fileSize = BigInt(file.size);

    const created = await this.prisma.attachments.create({
      data: {
        file_name: file.originalname,
        file_size: fileSize,
        mime_type: file.mimetype,
        storage_url: file.path, // path where multer saved it
        uploader_id: userId,
        item_id: itemId || null,
      },
    });
    return this.mapAttachment(created);
  }

  async findAllByItem(itemId: string) {
    const attachments = await this.prisma.attachments.findMany({
      where: { item_id: itemId },
    });
    
    return attachments.map(att => this.mapAttachment(att));
  }

  async findOne(id: string) {
    const attachment = await this.prisma.attachments.findUnique({
      where: { id },
    });

    if (!attachment) {
      throw new NotFoundException(`Attachment with ID ${id} not found.`);
    }

    return this.mapAttachment(attachment);
  }

  async download(id: string) {
    const attachment = await this.prisma.attachments.findUnique({
      where: { id },
    });
    if (!attachment) {
      throw new NotFoundException(`Attachment with ID ${id} not found.`);
    }
    const filePath = join(process.cwd(), attachment.storage_url);

    if (!fs.existsSync(filePath)) {
      throw new NotFoundException('File not found on the server');
    }

    const fileStream = createReadStream(filePath);
    return {
      stream: new StreamableFile(fileStream),
      fileName: attachment.file_name,
      mimeType: attachment.mime_type
    };
  }

  async remove(id: string) {
    const attachment = await this.prisma.attachments.findUnique({
      where: { id },
    });
    if (!attachment) {
      throw new NotFoundException(`Attachment with ID ${id} not found.`);
    }
    
    // Remove from file system
    const filePath = join(process.cwd(), attachment.storage_url);
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
    }

    return this.prisma.attachments.delete({
      where: { id },
    });
  }
}
