import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma.service';
import { CreateWorkspaceDto } from './dto/create-workspace.dto';
import { UpdateWorkspaceDto } from './dto/update-workspace.dto';
import { randomUUID } from 'crypto';

@Injectable()
export class WorkspaceService {
  constructor(private readonly prisma: PrismaService) {}

  private mapWorkspace(w: any) {
    return {
      id: w.id,
      name: w.name,
      slug: w.slug,
      logoUrl: w.logo_url || '',
      plan: w.plan || 'free',
      ownerId: w.owner_id,
      createdAt: w.created_at,
      updatedAt: w.updated_at,
      _count: w._count,
      workspaceMembers: w.workspace_members ? w.workspace_members.map((m: any) => this.mapMember(m)) : undefined,
    };
  }

  private mapMember(m: any) {
    const user = m.users_workspace_members_user_idTousers || {};
    return {
      id: m.id,
      workspaceId: m.workspace_id,
      userId: m.user_id,
      role: m.role,
      email: user.email || '',
      fullName: user.full_name || '',
      createdAt: m.created_at,
    };
  }

  private toDbModel(dto: any) {
    const dbData: any = {};
    if (dto.name !== undefined) dbData.name = dto.name;
    if (dto.slug !== undefined) dbData.slug = dto.slug;
    if (dto.logoUrl !== undefined) dbData.logo_url = dto.logoUrl;
    if (dto.plan !== undefined) dbData.plan = dto.plan;
    return dbData;
  }

  async create(createWorkspaceDto: CreateWorkspaceDto, userId: string) {
    const data = this.toDbModel(createWorkspaceDto);
    const ws = await this.prisma.workspaces.create({
      data: {
        ...data,
        owner_id: userId,
        workspace_members: {
          create: {
            user_id: userId,
            role: 'owner',
          },
        },
      },
    });
    return this.mapWorkspace(ws);
  }

  async findAll(userId: string) {
    const list = await this.prisma.workspaces.findMany({
      where: {
        OR: [
          { owner_id: userId },
          {
            workspace_members: {
              some: {
                user_id: userId,
              },
            },
          },
        ],
      },
      include: {
        _count: {
          select: {
            workspace_members: true,
            projects: true,
          }
        }
      }
    });
    return list.map(w => this.mapWorkspace(w));
  }

  async findOne(id: string, userId: string) {
    const workspace = await this.prisma.workspaces.findFirst({
      where: {
        id,
        OR: [
          { owner_id: userId },
          {
            workspace_members: {
              some: {
                user_id: userId,
              },
            },
          },
        ],
      },
      include: {
        workspace_members: {
          include: {
            users_workspace_members_user_idTousers: {
              select: {
                id: true,
                email: true,
                full_name: true,
              }
            }
          }
        }
      }
    });

    if (!workspace) {
      throw new NotFoundException(`Workspace with ID ${id} not found or you don't have access.`);
    }

    return this.mapWorkspace(workspace);
  }

  async update(id: string, updateWorkspaceDto: UpdateWorkspaceDto, userId: string) {
    await this.findOne(id, userId); // verify ownership/membership
    const data = this.toDbModel(updateWorkspaceDto);
    const ws = await this.prisma.workspaces.update({
      where: { id },
      data,
    });
    return this.mapWorkspace(ws);
  }

  async getMembers(id: string, userId: string) {
    await this.findOne(id, userId); // check access
    const members = await this.prisma.workspace_members.findMany({
      where: { workspace_id: id },
      include: {
        users_workspace_members_user_idTousers: {
          select: { id: true, full_name: true, email: true }
        }
      }
    });
    return members.map(m => this.mapMember(m));
  }

  async inviteMember(workspaceId: string, email: string, role: string) {
    if (!email || !email.trim()) {
      throw new Error("올바른 이메일 주소를 입력해 주세요.");
    }
    const trimmedEmail = email.trim();

    // 1. Check if user exists by email
    let user = await this.prisma.users.findUnique({
      where: { email: trimmedEmail }
    });

    if (!user) {
      const newUserId = randomUUID();
      const namePart = trimmedEmail.split("@")[0];
      const fullName = namePart.charAt(0).toUpperCase() + namePart.slice(1);

      user = await this.prisma.users.create({
        data: {
          id: newUserId,
          email: trimmedEmail,
          password_hash: "1234",
          full_name: fullName,
          is_active: true,
        }
      });
    }

    // 2. Check if already a member
    const existing = await this.prisma.workspace_members.findFirst({
      where: {
        workspace_id: workspaceId,
        user_id: user.id
      }
    });

    if (existing) {
      throw new Error("이미 해당 워크스페이스의 멤버로 등록된 사용자입니다.");
    }

    // 3. Insert member
    const memberId = randomUUID();
    const newMember = await this.prisma.workspace_members.create({
      data: {
        id: memberId,
        workspace_id: workspaceId,
        user_id: user.id,
        role: role || 'member',
      },
      include: {
        users_workspace_members_user_idTousers: {
          select: { id: true, full_name: true, email: true }
        }
      }
    });

    return this.mapMember(newMember);
  }

  async updateMemberRole(workspaceId: string, memberId: string, role: string) {
    if (!role || !role.trim()) {
      throw new Error("올바른 역할을 입력해 주세요.");
    }
    await this.prisma.workspace_members.update({
      where: { id: memberId },
      data: { role: role.trim() }
    });
  }

  async deleteMember(workspaceId: string, memberId: string) {
    await this.prisma.workspace_members.delete({
      where: { id: memberId }
    });
  }

  async remove(id: string, userId: string) {
    await this.findOne(id, userId); // verify access

    await this.prisma.workspaces.delete({
      where: { id },
    });
  }
}
