import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma.service';
import { CreateBoardDto } from './dto/create-board.dto';
import { UpdateBoardDto } from './dto/update-board.dto';

@Injectable()
export class BoardService {
  constructor(private readonly prisma: PrismaService) {}

  private mapBoard(b: any) {
    return {
      id: b.id,
      projectId: b.project_id,
      name: b.name,
      description: b.description,
      boardType: b.board_type,
      position: b.position,
      isArchived: b.is_archived,
      createdBy: b.created_by,
      createdAt: b.created_at,
      updatedAt: b.updated_at,
    };
  }

  private toDbModel(dto: any) {
    const dbData: any = {};
    if (dto.name !== undefined) dbData.name = dto.name;
    if (dto.description !== undefined) dbData.description = dto.description;
    if (dto.boardType !== undefined) dbData.board_type = dto.boardType;
    if (dto.position !== undefined) dbData.position = dto.position;
    if (dto.projectId !== undefined) dbData.project_id = dto.projectId;
    if (dto.isArchived !== undefined) dbData.is_archived = dto.isArchived;
    return dbData;
  }

  async create(workspaceId: string, createBoardDto: CreateBoardDto, userId: string) {
    const project = await this.prisma.projects.findFirst({
      where: {
        id: createBoardDto.projectId,
        workspace_id: workspaceId,
      }
    });

    if (!project) {
      throw new NotFoundException(`Project not found in the given workspace.`);
    }

    const data = this.toDbModel(createBoardDto);
    data.created_by = userId;

    const board = await this.prisma.boards.create({
      data,
    });
    return this.mapBoard(board);
  }

  async findAll(workspaceId: string, userId: string) {
    const workspace = await this.prisma.workspaces.findFirst({
      where: {
        id: workspaceId,
        OR: [
          { owner_id: userId },
          { workspace_members: { some: { user_id: userId } } }
        ]
      }
    });

    if (!workspace) {
      throw new NotFoundException(`Workspace not found or unauthorized access.`);
    }

    const boards = await this.prisma.boards.findMany({
      where: {
        projects: {
          workspace_id: workspaceId,
        }
      },
      orderBy: {
        position: 'asc'
      }
    });
    return boards.map(b => this.mapBoard(b));
  }

  async findAllByProject(projectId: string) {
    const boards = await this.prisma.boards.findMany({
      where: { project_id: projectId },
      orderBy: { position: 'asc' }
    });
    return boards.map(b => this.mapBoard(b));
  }

  async findOne(workspaceId: string, id: string, userId: string) {
    const board = await this.prisma.boards.findFirst({
      where: {
        id,
        projects: {
          workspace_id: workspaceId,
        }
      }
    });

    if (!board) {
      throw new NotFoundException(`Board with ID ${id} not found.`);
    }
    return this.mapBoard(board);
  }

  async findOneById(id: string) {
    const board = await this.prisma.boards.findUnique({
      where: { id }
    });
    if (!board) {
      throw new NotFoundException(`Board with ID ${id} not found.`);
    }
    return this.mapBoard(board);
  }

  async update(workspaceId: string, id: string, updateBoardDto: UpdateBoardDto, userId: string) {
    await this.findOne(workspaceId, id, userId); // check access
    
    const data = this.toDbModel(updateBoardDto);
    const board = await this.prisma.boards.update({
      where: { id },
      data,
    });
    return this.mapBoard(board);
  }

  async remove(workspaceId: string, id: string, userId: string) {
    await this.findOne(workspaceId, id, userId); // check access
    
    return this.prisma.boards.delete({
      where: { id },
    });
  }
}
