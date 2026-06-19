import { PrismaClient } from '@prisma/client';
import * as bcrypt from 'bcryptjs';

const prisma = new PrismaClient();

async function main() {
  const hash = await bcrypt.hash('password', 12);
  await prisma.users.update({
    where: { email: 'admin@example.com' },
    data: { password_hash: hash }
  });
  console.log('Password updated to: password');
}

main().catch(e => {
  console.error(e);
  process.exit(1);
}).finally(async () => {
  await prisma.$disconnect();
});
