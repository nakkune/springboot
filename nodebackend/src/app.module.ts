import { Module } from '@nestjs/common';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { PrismaModule } from './prisma.module';
import { UserModule } from './domains/user/user.module';
import { WorkspaceModule } from './domains/workspace/workspace.module';
import { BoardModule } from './domains/board/board.module';
import { ItemModule } from './domains/item/item.module';
import { ItemValueModule } from './domains/item-value/item-value.module';
import { AttachmentModule } from './domains/attachment/attachment.module';
import { ErpEmployeeModule } from './domains/erp-employee/erp-employee.module';
import { ActivityLogModule } from './domains/activity-log/activity-log.module';
import { ProjectModule } from './domains/project/project.module';
import { BoardColumnModule } from './domains/board-column/board-column.module';
import { BoardGroupModule } from './domains/board-group/board-group.module';
import { DepartmentModule } from './domains/department/department.module';
import { PayrollModule } from './domains/payroll/payroll.module';
import { QuotationModule } from './domains/quotation/quotation.module';
import { TaxInvoiceModule } from './domains/tax-invoice/tax-invoice.module';
import { NotificationModule } from './domains/notification/notification.module';
import { CodeModule } from './domains/code/code.module';
import { AttendanceModule } from './domains/attendance/attendance.module';
import { LeaveModule } from './domains/leave/leave.module';

@Module({
  imports: [PrismaModule, UserModule, WorkspaceModule, BoardModule, ItemModule, ItemValueModule, AttachmentModule, ErpEmployeeModule, ActivityLogModule, ProjectModule, BoardColumnModule, BoardGroupModule, DepartmentModule, PayrollModule, QuotationModule, TaxInvoiceModule, NotificationModule, CodeModule, AttendanceModule, LeaveModule],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
