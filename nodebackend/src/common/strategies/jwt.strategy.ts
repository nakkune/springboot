import { Injectable, UnauthorizedException } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';
import { PrismaService } from '../../prisma.service';

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy) {
  constructor(private readonly prisma: PrismaService) {
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      ignoreExpiration: false,
      secretOrKey: process.env.JWT_SECRET || 'NakProjectOSNodeBackendSuperSecretKey',
    });
  }

  async validate(payload: { id: string; email: string }) {
    const user = await this.prisma.users.findUnique({
      where: { id: payload.id },
    });

    if (!user || !user.is_active) {
      throw new UnauthorizedException('인증되지 않았거나 비활성화된 계정입니다.');
    }

    return {
      id: user.id,
      email: user.email,
      fullName: user.full_name,
      role: user.role,
    };
  }
}
