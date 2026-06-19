import { Injectable, UnauthorizedException, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma.service';
import { JwtService } from '@nestjs/jwt';
import { LoginDto } from './dto/login.dto';
import { UpdateUserDto } from './dto/update-user.dto';
import * as bcrypt from 'bcryptjs';

@Injectable()
export class UserService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly jwtService: JwtService,
  ) {}

  async login(loginDto: LoginDto) {
    const { email, password } = loginDto;

    const user = await this.prisma.users.findUnique({
      where: { email },
    });

    if (!user) {
      throw new UnauthorizedException('이메일 또는 비밀번호가 올바르지 않습니다.');
    }

    if (!user.is_active) {
      throw new UnauthorizedException('비활성화된 계정입니다. 관리자에게 문의하세요.');
    }

    if (!user.password_hash) {
      throw new UnauthorizedException('이메일 또는 비밀번호가 올바르지 않습니다.');
    }

    const isPasswordValid = await bcrypt.compare(password, user.password_hash);
    if (!isPasswordValid) {
      throw new UnauthorizedException('이메일 또는 비밀번호가 올바르지 않습니다.');
    }

    const payload = { id: user.id, email: user.email };
    const token = this.jwtService.sign(payload);

    const { password_hash, ...userWithoutPassword } = user;
    return {
      success: true,
      token,
      user: {
        id: userWithoutPassword.id,
        email: userWithoutPassword.email,
        fullName: userWithoutPassword.full_name,
        avatarUrl: userWithoutPassword.avatar_url,
        timezone: userWithoutPassword.timezone,
        theme: userWithoutPassword.theme || 'dark',
        role: userWithoutPassword.role,
        isActive: userWithoutPassword.is_active,
        createdAt: userWithoutPassword.created_at,
        updatedAt: userWithoutPassword.updated_at,
      },
    };
  }

  async getAllUsers() {
    const users = await this.prisma.users.findMany({
      orderBy: { created_at: 'desc' },
    });
    return users.map(({ password_hash, ...u }) => ({
      id: u.id,
      email: u.email,
      fullName: u.full_name,
      avatarUrl: u.avatar_url,
      timezone: u.timezone,
      theme: u.theme || 'dark',
      role: u.role,
      isActive: u.is_active,
      createdAt: u.created_at,
      updatedAt: u.updated_at,
    }));
  }

  async getUserById(id: string) {
    const user = await this.prisma.users.findUnique({
      where: { id },
    });
    if (!user) {
      throw new NotFoundException('해당 사용자를 찾을 수 없습니다.');
    }
    const { password_hash, ...u } = user;
    return {
      id: u.id,
      email: u.email,
      fullName: u.full_name,
      avatarUrl: u.avatar_url,
      timezone: u.timezone,
      theme: u.theme || 'dark',
      role: u.role,
      isActive: u.is_active,
      createdAt: u.created_at,
      updatedAt: u.updated_at,
    };
  }

  async updateUser(id: string, dto: UpdateUserDto) {
    const existing = await this.prisma.users.findUnique({ where: { id } });
    if (!existing) {
      throw new NotFoundException('해당 사용자를 찾을 수 없습니다.');
    }

    const data: any = {};
    if (dto.fullName !== undefined) data.full_name = dto.fullName;
    if (dto.avatarUrl !== undefined) data.avatar_url = dto.avatarUrl;
    if (dto.timezone !== undefined) data.timezone = dto.timezone;
    if (dto.theme !== undefined) data.theme = dto.theme;

    const updated = await this.prisma.users.update({
      where: { id },
      data,
    });

    const { password_hash, ...u } = updated;
    return {
      id: u.id,
      email: u.email,
      fullName: u.full_name,
      avatarUrl: u.avatar_url,
      timezone: u.timezone,
      theme: u.theme || 'dark',
      role: u.role,
      isActive: u.is_active,
      createdAt: u.created_at,
      updatedAt: u.updated_at,
    };
  }
}
