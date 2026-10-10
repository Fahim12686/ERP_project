package com.intern.erp.config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.intern.erp.finance.model.Account;
import com.intern.erp.finance.model.Expense;
import com.intern.erp.finance.model.Invoice;
import com.intern.erp.finance.model.PayrollEntry;
import com.intern.erp.finance.model.enums.AccountType;
import com.intern.erp.finance.model.enums.ExpenseCategory;
import com.intern.erp.finance.model.enums.PaymentStatus;
import com.intern.erp.finance.repository.AccountRepository;
import com.intern.erp.finance.repository.ExpenseRepository;
import com.intern.erp.finance.repository.InvoiceRepository;
import com.intern.erp.finance.repository.JournalEntryRepository;
import com.intern.erp.finance.repository.LedgerRepository;
import com.intern.erp.finance.repository.PayrollEntryRepository;
import com.intern.erp.hr.model.Attendance;
import com.intern.erp.hr.model.Department;
import com.intern.erp.hr.model.Employee;
import com.intern.erp.hr.model.LeaveBalance;
import com.intern.erp.hr.model.LeaveRequest;
import com.intern.erp.hr.model.Position;
import com.intern.erp.hr.model.enums.AttendanceStatus;
import com.intern.erp.hr.model.enums.EmployeeStatus;
import com.intern.erp.hr.model.enums.EmployeeType;
import com.intern.erp.hr.model.enums.LeaveStatus;
import com.intern.erp.hr.model.enums.LeaveType;
import com.intern.erp.hr.repository.AttendanceRepository;
import com.intern.erp.hr.repository.DepartmentRepository;
import com.intern.erp.hr.repository.EmployeeRepository;
import com.intern.erp.hr.repository.LeaveBalanceRepository;
import com.intern.erp.hr.repository.LeaveRequestRepository;
import com.intern.erp.hr.repository.PositionRepository;
import com.intern.erp.users.model.UserAccount;
import com.intern.erp.users.repository.UserAccountRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    // HR Repositories
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private PositionRepository positionRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private LeaveRequestRepository leaveRequestRepository;
    @Autowired private LeaveBalanceRepository leaveBalanceRepository;

    // Finance Repositories
    @Autowired private AccountRepository accountRepository;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private LedgerRepository ledgerRepository;
    @Autowired private PayrollEntryRepository payrollEntryRepository;
    @Autowired(required = false) private SequenceGeneratorService sequenceGeneratorService;

    // User Account Repository
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private final Random random = new Random();

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🚀 Starting Data Initialization check...");

        // Idempotency guard: if any core collection already has data, skip seeding
        // Note: userAccountRepository is excluded so demo accounts are always updated/created
        boolean alreadySeeded = departmentRepository.count() > 0
                || accountRepository.count() > 0
                || employeeRepository.count() > 0;

        if (alreadySeeded) {
            System.out.println("ℹ️ Existing data detected (skipping business data seeding). Set environment variable ERP_SEED_FORCE=1 to force reseed.");
            // Optional force reseed flag
            String force = System.getenv("ERP_SEED_FORCE");
            if (force == null || !force.equals("1")) {
                // Always initialize/update demo users even if business data exists
                initializeUsers();
                System.out.println("✅ Demo users initialized/updated. Business data seeding skipped.");
                return; // Skip business data seeding but return after updating users
            }
            System.out.println("⚠️ Force seeding enabled. Clearing existing data...");
            clearExistingData();
        } else {
            System.out.println("📦 No existing data detected. Seeding fresh dataset...");
        }

        // Proceed with seeding (fresh or forced)
        List<Department> departments = initializeDepartments();
        List<Position> positions = initializePositions(departments);
        List<Employee> employees = initializeEmployees(departments, positions);
        List<Account> accounts = initializeAccounts();

        initializeAttendance(employees);
        initializeLeaveBalances(employees);
        initializeLeaveRequests(employees);
        initializeExpenses(accounts);
        initializeInvoices(accounts);
        initializePayroll(employees, accounts);
        initializeAdditionalFinanceData(accounts);
        initializeUsers();

        System.out.println("✅ Data Initialization Completed Successfully!");
        printDataSummary();
    }

    private void clearExistingData() {
        System.out.println("🧹 Clearing existing data (forced)...");
        // HR
        leaveBalanceRepository.deleteAll();
        leaveRequestRepository.deleteAll();
        attendanceRepository.deleteAll();
        employeeRepository.deleteAll();
        positionRepository.deleteAll();
        departmentRepository.deleteAll();
        // Finance
        ledgerRepository.deleteAll();
        journalEntryRepository.deleteAll();
        payrollEntryRepository.deleteAll();
        invoiceRepository.deleteAll();
        expenseRepository.deleteAll();
        accountRepository.deleteAll();
        // Users
        userAccountRepository.deleteAll();
        System.out.println("✅ Existing data cleared");
    }

    private List<Department> initializeDepartments() {
        System.out.println("📁 Creating Departments...");
        
        Department[] deptData = {
            new Department(null, "Engineering", "Software Development and Technology", null, true, LocalDateTime.now(), LocalDateTime.now()),
            new Department(null, "Human Resources", "Employee Management and Relations", null, true, LocalDateTime.now(), LocalDateTime.now()),
            new Department(null, "Finance", "Financial Management and Accounting", null, true, LocalDateTime.now(), LocalDateTime.now()),
            new Department(null, "Sales", "Business Development and Client Relations", null, true, LocalDateTime.now(), LocalDateTime.now()),
            new Department(null, "Marketing", "Brand Management and Customer Acquisition", null, true, LocalDateTime.now(), LocalDateTime.now()),
            new Department(null, "Operations", "Business Operations and Process Management", null, true, LocalDateTime.now(), LocalDateTime.now())
        };
        
        List<Department> departments = departmentRepository.saveAll(Arrays.asList(deptData));
        System.out.println("✅ Created " + departments.size() + " departments");
        return departments;
    }

    private List<Position> initializePositions(List<Department> departments) {
        System.out.println("💼 Creating Positions...");
        
        Position[] positions = {
            // Engineering
            new Position(null, "Software Engineer", "Develops software applications", departments.get(0).getId(), new BigDecimal("5000"), new BigDecimal("10000"), true, LocalDateTime.now(), LocalDateTime.now()),
            new Position(null, "Senior Software Engineer", "Senior developer with leadership responsibilities", departments.get(0).getId(), new BigDecimal("8000"), new BigDecimal("15000"), true, LocalDateTime.now(), LocalDateTime.now()),
            new Position(null, "Engineering Manager", "Manages engineering team and projects", departments.get(0).getId(), new BigDecimal("12000"), new BigDecimal("20000"), true, LocalDateTime.now(), LocalDateTime.now()),
            
            // HR
            new Position(null, "HR Specialist", "Handles recruitment and employee relations", departments.get(1).getId(), new BigDecimal("4000"), new BigDecimal("7000"), true, LocalDateTime.now(), LocalDateTime.now()),
            new Position(null, "HR Manager", "Manages HR operations and policies", departments.get(1).getId(), new BigDecimal("8000"), new BigDecimal("12000"), true, LocalDateTime.now(), LocalDateTime.now()),
            
            // Finance
            new Position(null, "Accountant", "Manages financial records and transactions", departments.get(2).getId(), new BigDecimal("4500"), new BigDecimal("8000"), true, LocalDateTime.now(), LocalDateTime.now()),
            new Position(null, "Finance Manager", "Oversees financial operations", departments.get(2).getId(), new BigDecimal("9000"), new BigDecimal("14000"), true, LocalDateTime.now(), LocalDateTime.now()),
            
            // Sales
            new Position(null, "Sales Executive", "Handles client relationships and sales", departments.get(3).getId(), new BigDecimal("3500"), new BigDecimal("7000"), true, LocalDateTime.now(), LocalDateTime.now()),
            new Position(null, "Sales Manager", "Manages sales team and strategies", departments.get(3).getId(), new BigDecimal("7000"), new BigDecimal("13000"), true, LocalDateTime.now(), LocalDateTime.now()),
            
            // Marketing
            new Position(null, "Marketing Specialist", "Executes marketing campaigns", departments.get(4).getId(), new BigDecimal("4000"), new BigDecimal("8000"), true, LocalDateTime.now(), LocalDateTime.now()),
            
            // Operations
            new Position(null, "Operations Analyst", "Analyzes and improves operations", departments.get(5).getId(), new BigDecimal("5000"), new BigDecimal("9000"), true, LocalDateTime.now(), LocalDateTime.now())
        };
        
        List<Position> savedPositions = positionRepository.saveAll(Arrays.asList(positions));
        System.out.println("✅ Created " + savedPositions.size() + " positions");
        return savedPositions;
    }

    private List<Employee> initializeEmployees(List<Department> departments, List<Position> positions) {
        System.out.println("👥 Creating Employees...");
        
        Employee[] employeeData = {
            // Engineering Team
            createEmployee("EMP001", "Rahim", "Ahmed", "rahim.ahmed@company.com", "+880-1712345678", departments.get(0).getId(), positions.get(1).getId(), null, new BigDecimal("9500"), EmployeeType.FULL_TIME, EmployeeStatus.ACTIVE, "2022-01-15"),
            createEmployee("EMP002", "Fatima", "Begum", "fatima.begum@company.com", "+880-1812345678", departments.get(0).getId(), positions.get(0).getId(), null, new BigDecimal("7500"), EmployeeType.FULL_TIME, EmployeeStatus.ACTIVE, "2022-03-20"),
            createEmployee("EMP003", "Karim", "Hossain", "karim.hossain@company.com", "+880-1912345678", departments.get(0).getId(), positions.get(2).getId(), null, new BigDecimal("14000"), EmployeeType.FULL_TIME, EmployeeStatus.ACTIVE, "2021-11-10"),
            createEmployee("EMP004", "Ayesha", "Khan", "ayesha.khan@company.com", "+880-1612345678", departments.get(0).getId(), positions.get(0).getId(), "EMP003", new BigDecimal("7200"), EmployeeType.FULL_TIME, EmployeeStatus.ACTIVE, "2023-02-14"),
            
            // HR Team
            createEmployee("EMP005", "Jamal", "Uddin", "jamal.uddin@company.com", "+880-1512345678", departments.get(1).getId(), positions.get(4).getId(), null, new BigDecimal("9500"), EmployeeType.FULL_TIME, EmployeeStatus.ACTIVE, "2021-08-22"),
            createEmployee("EMP006", "Nasreen", "Akter", "nasreen.akter@company.com", "+880-1312345678", departments.get(1).getId(), positions.get(3).getId(), "EMP005", new BigDecimal("5800"), EmployeeType.FULL_TIME, EmployeeStatus.ACTIVE, "2022-06-15"),
            
            // Finance Team
            createEmployee("EMP007", "Habib", "Rahman", "habib.rahman@company.com", "+880-1112345678", departments.get(2).getId(), positions.get(6).getId(), null, new BigDecimal("12000"), EmployeeType.FULL_TIME, EmployeeStatus.ACTIVE, "2021-09-30"),
            createEmployee("EMP008", "Rashida", "Begum", "rashida.begum@company.com", "+880-1212345678", departments.get(2).getId(), positions.get(5).getId(), "EMP007", new BigDecimal("6500"), EmployeeType.FULL_TIME, EmployeeStatus.ACTIVE, "2022-11-08"),
            
            // Sales Team
            createEmployee("EMP009", "Tanvir", "Islam", "tanvir.islam@company.com", "+880-1811123456", departments.get(3).getId(), positions.get(8).getId(), null, new BigDecimal("8800"), EmployeeType.FULL_TIME, EmployeeStatus.ACTIVE, "2022-01-03"),
            createEmployee("EMP010", "Shamima", "Parveen", "shamima.parveen@company.com", "+880-1711123456", departments.get(3).getId(), positions.get(7).getId(), "EMP009", new BigDecimal("5200"), EmployeeType.FULL_TIME, EmployeeStatus.ACTIVE, "2022-07-19"),
            
            // Marketing Team
            createEmployee("EMP011", "Sohel", "Miah", "sohel.miah@company.com", "+880-1611123456", departments.get(4).getId(), positions.get(9).getId(), null, new BigDecimal("6200"), EmployeeType.FULL_TIME, EmployeeStatus.ACTIVE, "2022-04-12"),
            
            // Operations Team
            createEmployee("EMP012", "Tahmina", "Sultana", "tahmina.sultana@company.com", "+880-1511123456", departments.get(5).getId(), positions.get(10).getId(), null, new BigDecimal("7500"), EmployeeType.FULL_TIME, EmployeeStatus.ACTIVE, "2022-05-28"),
            
            // Part-time and Contract employees
            createEmployee("EMP013", "Rafiq", "Haque", "rafiq.haque@company.com", "+880-1911123456", departments.get(0).getId(), positions.get(0).getId(), "EMP001", new BigDecimal("4500"), EmployeeType.PART_TIME, EmployeeStatus.ACTIVE, "2023-01-10"),
            createEmployee("EMP014", "Rehana", "Khatun", "rehana.khatun@company.com", "+880-1311123456", departments.get(4).getId(), positions.get(9).getId(), null, new BigDecimal("5800"), EmployeeType.CONTRACT, EmployeeStatus.ACTIVE, "2023-03-15"),
            createEmployee("EMP015", "Imran", "Hossain", "imran.hossain@company.com", "+880-1111123456", departments.get(0).getId(), positions.get(0).getId(), "EMP003", new BigDecimal("3500"), EmployeeType.INTERN, EmployeeStatus.ACTIVE, "2023-06-01"),
         createEmployee("EMP016", "Fahim", "Islam", "99220040214@klu.ac.in", "+880-6302313370", departments.get(0).getId(), positions.get(0).getId(), "EMP003", new BigDecimal("3500"), EmployeeType.INTERN, EmployeeStatus.ACTIVE, "2023-06-01"),

        };
        
        List<Employee> employees = employeeRepository.saveAll(Arrays.asList(employeeData));
        
        // Update department heads
        departments.get(0).setHeadEmployeeId(employees.get(2).getId()); // Karim Hossain - Engineering Manager
        departments.get(1).setHeadEmployeeId(employees.get(4).getId()); // Jamal Uddin - HR Manager
        departments.get(2).setHeadEmployeeId(employees.get(6).getId()); // Habib Rahman - Finance Manager
        departments.get(3).setHeadEmployeeId(employees.get(8).getId()); // Tanvir Islam - Sales Manager
        departments.get(4).setHeadEmployeeId(employees.get(10).getId()); // Sohel Miah - Marketing Specialist
        departments.get(5).setHeadEmployeeId(employees.get(11).getId()); // Tahmina Sultana - Operations Analyst
        
        departmentRepository.saveAll(departments);
        
        System.out.println("✅ Created " + employees.size() + " employees");
        return employees;
    }

    private Employee createEmployee(String empId, String firstName, String lastName, String email, String phone, 
                                  String deptId, String posId, String managerId, BigDecimal salary, 
                                  EmployeeType type, EmployeeStatus status, String joinDate) {
        Employee emp = new Employee();
        emp.setEmployeeId(empId);
        emp.setFirstName(firstName);
        emp.setLastName(lastName);
        emp.setEmail(email);
        emp.setPhone(phone);
        emp.setAddress(generateRandomAddress());
        emp.setDateOfBirth(LocalDate.parse(joinDate).minusYears(25 + random.nextInt(15)));
        emp.setJoinDate(LocalDate.parse(joinDate));
        emp.setDepartmentId(deptId);
        emp.setPositionId(posId);
        emp.setManagerId(managerId);
        emp.setSalary(salary);
        emp.setEmployeeType(type);
        emp.setStatus(status);
        emp.setEmergencyContactName(firstName + " Family");
        emp.setEmergencyContactPhone("+880-" + String.format("%010d", random.nextInt(1000000000)));
        emp.setBankAccountNumber("ACC" + String.format("%08d", random.nextInt(100000000)));
        emp.setTaxId("TAX" + String.format("%09d", random.nextInt(1000000000)));
        emp.setCreatedAt(LocalDateTime.now());
        emp.setUpdatedAt(LocalDateTime.now());
        return emp;
    }

    private List<Account> initializeAccounts() {
        System.out.println("🏦 Creating Chart of Accounts...");
        
        Account[] accountData = {
            // Assets
            new Account(1001L, "Cash", "1001", AccountType.ASSET, "Company Cash Account", true, "99220040214@klu.ac.in"),
            new Account(1002L, "Bank Account", "1002", AccountType.ASSET, "Company Bank Account", true, "99220040214@klu.ac.in"),
            new Account(1003L, "Accounts Receivable", "1003", AccountType.ASSET, "Money owed by customers", true, "99220040214@klu.ac.in"),
            new Account(1004L, "Office Equipment", "1004", AccountType.ASSET, "Office furniture and equipment", true, "99220040214@klu.ac.in"),

            // Liabilities
            new Account(2001L, "Accounts Payable", "2001", AccountType.LIABILITY, "Money owed to suppliers", true, "99220040214@klu.ac.in"),
            new Account(2002L, "Salary Payable", "2002", AccountType.LIABILITY, "Unpaid salaries", true, "99220040214@klu.ac.in"),
            new Account(2003L, "Tax Payable", "2003", AccountType.LIABILITY, "Taxes owed", true, "99220040214@klu.ac.in"),

            // Equity
            new Account(3001L, "Owner's Equity", "3001", AccountType.EQUITY, "Owner's investment in business", true, "99220040214@klu.ac.in"),
            new Account(3002L, "Retained Earnings", "3002", AccountType.EQUITY, "Accumulated profits", true, "99220040214@klu.ac.in"),

            // Income
            new Account(4001L, "Service Revenue", "4001", AccountType.INCOME, "Revenue from services", true, "99220040214@klu.ac.in"),
            new Account(4002L, "Consulting Revenue", "4002", AccountType.INCOME, "Revenue from consulting", true, "99220040214@klu.ac.in"),

            // Expenses
            new Account(5001L, "Salary Expense", "5001", AccountType.EXPENSE, "Employee salaries", true, "99220040214@klu.ac.in"),
            new Account(5002L, "Office Rent", "5002", AccountType.EXPENSE, "Monthly office rent", true, "99220040214@klu.ac.in"),
            new Account(5003L, "Utilities Expense", "5003", AccountType.EXPENSE, "Electricity, water, internet", true, "99220040214@klu.ac.in"),
            new Account(5004L, "Travel Expense", "5004", AccountType.EXPENSE, "Business travel costs", true, "99220040214@klu.ac.in"),
            new Account(5005L, "Office Supplies", "5005", AccountType.EXPENSE, "Stationery and supplies", true, "99220040214@klu.ac.in"),
            new Account(5006L, "Marketing Expense", "5006", AccountType.EXPENSE, "Advertising and marketing", true, "99220040214@klu.ac.in")
        };
        
        List<Account> accounts = accountRepository.saveAll(Arrays.asList(accountData));
        System.out.println("✅ Created " + accounts.size() + " accounts");
        return accounts;
    }

    private void initializeAttendance(List<Employee> employees) {
        System.out.println("📅 Creating Attendance Records...");
        
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();
        
        int attendanceCount = 0;
        for (Employee employee : employees) {
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                // Skip weekends
                if (date.getDayOfWeek().getValue() > 5) continue;
                
                AttendanceStatus status = generateAttendanceStatus();
                Attendance attendance = new Attendance();
                attendance.setEmployeeId(employee.getId());
                attendance.setDate(date);
                attendance.setStatus(status);
                
                if (status != AttendanceStatus.ABSENT) {
                    LocalTime checkIn = LocalTime.of(8 + random.nextInt(2), random.nextInt(60));
                    LocalTime checkOut = checkIn.plusHours(8 + random.nextInt(3)).plusMinutes(random.nextInt(60));
                    
                    attendance.setCheckInTime(checkIn);
                    attendance.setCheckOutTime(checkOut);
                    attendance.setWorkingHours((long) java.time.Duration.between(checkIn, checkOut).toMinutes());
                    attendance.setBreakTime(60L + random.nextInt(30)); // 60-90 minutes break
                }
                
                attendance.setLocation(random.nextBoolean() ? "Office" : "Work From Home");
                attendance.setNotes(generateAttendanceNotes(status));
                attendance.setCreatedAt(LocalDateTime.now());
                attendance.setUpdatedAt(LocalDateTime.now());
                
                attendanceRepository.save(attendance);
                attendanceCount++;
            }
        }
        
        System.out.println("✅ Created " + attendanceCount + " attendance records");
    }

    private void initializeLeaveBalances(List<Employee> employees) {
        System.out.println("🏖️ Creating Leave Balances...");
        
        int balanceCount = 0;
        for (Employee employee : employees) {
            for (LeaveType leaveType : LeaveType.values()) {
                LeaveBalance balance = new LeaveBalance();
                balance.setEmployeeId(employee.getId());
                balance.setLeaveType(leaveType);
                balance.setYear(2025);
                balance.setTotalDays(getTotalLeaveAllowance(leaveType, employee.getEmployeeType()));
                balance.setUsedDays(random.nextLong(balance.getTotalDays() / 2)); // Random used days
                balance.setLastUpdated(LocalDateTime.now());
                
                leaveBalanceRepository.save(balance);
                balanceCount++;
            }
        }
        
        System.out.println("✅ Created " + balanceCount + " leave balance records");
    }

    private void initializeLeaveRequests(List<Employee> employees) {
        System.out.println("📋 Creating Leave Requests...");
        
        int requestCount = 0;
        for (Employee employee : employees) {
            // Create 2-4 leave requests per employee
            int numRequests = 2 + random.nextInt(3);
            
            for (int i = 0; i < numRequests; i++) {
                LeaveRequest request = new LeaveRequest();
                request.setEmployeeId(employee.getId());
                request.setLeaveType(LeaveType.values()[random.nextInt(LeaveType.values().length)]);
                
                LocalDate startDate = LocalDate.now().plusDays(random.nextInt(180) - 90); // ±3 months
                LocalDate endDate = startDate.plusDays(1 + random.nextInt(10)); // 1-10 days
                
                request.setStartDate(startDate);
                request.setEndDate(endDate);
                request.setTotalDays((long) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1);
                request.setReason(generateLeaveReason(request.getLeaveType()));
                request.setStatus(generateLeaveStatus());
                
                if (request.getStatus() != LeaveStatus.PENDING) {
                    request.setApprovedBy(employee.getManagerId());
                    request.setApprovedAt(LocalDateTime.now().minusDays(random.nextInt(30)));
                }
                
                if (request.getStatus() == LeaveStatus.REJECTED) {
                    request.setRejectionReason("Insufficient coverage during requested period");
                }
                
                request.setAppliedAt(LocalDateTime.now().minusDays(random.nextInt(60)));
                request.setUpdatedAt(LocalDateTime.now());
                
                leaveRequestRepository.save(request);
                requestCount++;
            }
        }
        
        System.out.println("✅ Created " + requestCount + " leave requests");
    }

    private void initializeExpenses(List<Account> accounts) {
        System.out.println("💸 Creating Expense Records...");
        
        Account cashAccount = accounts.stream().filter(a -> a.getName().equals("Cash")).findFirst().orElse(null);
        Account rentAccount = accounts.stream().filter(a -> a.getName().equals("Office Rent")).findFirst().orElse(null);
        Account utilitiesAccount = accounts.stream().filter(a -> a.getName().equals("Utilities Expense")).findFirst().orElse(null);
        Account travelAccount = accounts.stream().filter(a -> a.getName().equals("Travel Expense")).findFirst().orElse(null);
        Account suppliesAccount = accounts.stream().filter(a -> a.getName().equals("Office Supplies")).findFirst().orElse(null);
        Expense[] expenses = {
            createExpense("Office Rent - September 2025", "Monthly office space rental", new BigDecimal("2500.00"), ExpenseCategory.RENT, rentAccount, cashAccount, PaymentStatus.PAID, LocalDate.of(2025, 9, 1)),
            createExpense("Office Rent - August 2025", "Monthly office space rental", new BigDecimal("2500.00"), ExpenseCategory.RENT, rentAccount, cashAccount, PaymentStatus.PAID, LocalDate.of(2025, 8, 1)),
            createExpense("Electricity Bill - August", "Monthly electricity bill", new BigDecimal("450.00"), ExpenseCategory.UTILITIES, utilitiesAccount, cashAccount, PaymentStatus.PAID, LocalDate.of(2025, 8, 15)),
            createExpense("Internet & Phone Services", "Monthly communication services", new BigDecimal("250.00"), ExpenseCategory.UTILITIES, utilitiesAccount, cashAccount, PaymentStatus.PAID, LocalDate.of(2025, 9, 5)),
            createExpense("Business Travel - Client Meeting", "Travel expenses for client presentation", new BigDecimal("850.00"), ExpenseCategory.TRAVEL, travelAccount, cashAccount, PaymentStatus.PENDING, LocalDate.of(2025, 9, 10)),
            createExpense("Office Supplies - Stationery", "Monthly office supplies purchase", new BigDecimal("180.00"), ExpenseCategory.OTHER, suppliesAccount, cashAccount, PaymentStatus.PAID, LocalDate.of(2025, 9, 12)),
            createExpense("Software Licenses", "Annual software licensing fees", new BigDecimal("1500.00"), ExpenseCategory.OTHER, suppliesAccount, cashAccount, PaymentStatus.PENDING, LocalDate.of(2025, 9, 8)),
            createExpense("Team Lunch - Project Completion", "Team celebration lunch", new BigDecimal("120.00"), ExpenseCategory.OTHER, suppliesAccount, cashAccount, PaymentStatus.PAID, LocalDate.of(2025, 9, 13)),
        };
        for (Expense e : expenses) {
            if (e.getId() == null) {
                long seq = sequenceGeneratorService.getSequenceNumber(Expense.class.getSimpleName());
                e.setId(seq);
            }
            System.out.println("[INIT] Expense prepared id=" + e.getId() + " title=" + e.getTitle());
        }
        expenseRepository.saveAll(Arrays.asList(expenses));
        System.out.println("✅ Created " + expenses.length + " expense records");
    }

    private void initializeInvoices(List<Account> accounts) {
        System.out.println("📄 Creating Invoice Records...");
        
        Account receivableAccount = accounts.stream().filter(a -> a.getName().equals("Accounts Receivable")).findFirst().orElse(null);
        Account serviceRevenueAccount = accounts.stream().filter(a -> a.getName().equals("Service Revenue")).findFirst().orElse(null);
        Account consultingRevenueAccount = accounts.stream().filter(a -> a.getName().equals("Consulting Revenue")).findFirst().orElse(null);
        Invoice[] invoices = {
            createInvoice("INV-2025-001", "TechCorp Solutions", "finance@techcorp.com", new BigDecimal("7500.00"), new BigDecimal("1350.00"), receivableAccount, serviceRevenueAccount, PaymentStatus.PAID, LocalDate.of(2025, 8, 15), LocalDate.of(2025, 9, 14)),
            createInvoice("INV-2025-002", "Global Industries Ltd", "accounting@global-ind.com", new BigDecimal("11250.00"), new BigDecimal("2025.00"), receivableAccount, consultingRevenueAccount, PaymentStatus.PAID, LocalDate.of(2025, 8, 20), LocalDate.of(2025, 9, 19)),
            createInvoice("INV-2025-003", "StartUp Innovations", "payments@startup-inn.com", new BigDecimal("4375.00"), new BigDecimal("787.50"), receivableAccount, serviceRevenueAccount, PaymentStatus.PENDING, LocalDate.of(2025, 9, 1), LocalDate.of(2025, 10, 1)),
            createInvoice("INV-2025-004", "Enterprise Systems Co", "billing@enterprise-sys.com", new BigDecimal("9100.00"), new BigDecimal("1638.00"), receivableAccount, consultingRevenueAccount, PaymentStatus.PENDING, LocalDate.of(2025, 9, 5), LocalDate.of(2025, 10, 5)),
            createInvoice("INV-2025-005", "Digital Marketing Pro", "finance@digimkt.com", new BigDecimal("6300.00"), new BigDecimal("1134.00"), receivableAccount, serviceRevenueAccount, PaymentStatus.PAID, LocalDate.of(2025, 9, 10), LocalDate.of(2025, 10, 10)),
            createInvoice("INV-2025-006", "Healthcare Solutions", "accounts@healthsol.com", new BigDecimal("13650.00"), new BigDecimal("2457.00"), receivableAccount, consultingRevenueAccount, PaymentStatus.PENDING, LocalDate.of(2025, 9, 12), LocalDate.of(2025, 10, 12)),
        };
        for (Invoice i : invoices) {
            if (i.getId() == null) {
                i.setId(sequenceGeneratorService.getSequenceNumber(Invoice.class.getSimpleName()));
            }
            System.out.println("[INIT] Invoice prepared id=" + i.getId() + " number=" + i.getInvoiceNumber());
        }
        invoiceRepository.saveAll(Arrays.asList(invoices));
        System.out.println("✅ Created " + invoices.length + " invoice records");
    }

    private void initializePayroll(List<Employee> employees, List<Account> accounts) {
        System.out.println("💰 Creating Payroll Records...");
        for (Employee employee : employees) {
            PayrollEntry payroll = new PayrollEntry();
            if (payroll.getId() == null) {
                payroll.setId(sequenceGeneratorService.getSequenceNumber(PayrollEntry.class.getSimpleName()));
            }
            payroll.setEmployeeId(employee.getId()); // Set as String (MongoDB ObjectId)
            payroll.setEmployeeCode(employee.getEmployeeId()); // Set business employee code (EMP001, etc.)
            payroll.setEmployeeName(employee.getFullName());
            payroll.setGrossSalary(employee.getSalary().divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP));
            BigDecimal deductions = payroll.getGrossSalary().multiply(new BigDecimal("0.22")).setScale(2, RoundingMode.HALF_UP);
            payroll.setDeductions(deductions);
            payroll.setNetSalary(payroll.getGrossSalary().subtract(deductions).setScale(2, RoundingMode.HALF_UP));
            payroll.setPayDate(LocalDate.of(2025, 9, 30));
            payroll.setStatus(com.intern.erp.finance.model.enums.PaymentStatus.PAID);
            System.out.println("[INIT] Payroll prepared id=" + payroll.getId() + " emp=" + payroll.getEmployeeName());
            payrollEntryRepository.save(payroll);
        }
        System.out.println("✅ Created payroll records for " + employees.size() + " employees");
    }

    private void initializeUsers() {
        System.out.println("👤 Seeding/Updating user accounts...");
        
        // Define demo accounts
        String[][] demoAccounts = {
            {"admin", "admin@company.com", "ADMIN"},
            {"hr", "hr@company.com", "HR"},
            {"finance", "finance@company.com", "FINANCE"}
        };
        
        String demoPassword = "demo123";
        String encodedPassword = passwordEncoder.encode(demoPassword);
        
        for (String[] account : demoAccounts) {
            String username = account[0];
            String email = account[1];
            String role = account[2];
            
            // Check if user exists by email or username
            Optional<UserAccount> existingUser = userAccountRepository.findByEmail(email);
            if (existingUser.isEmpty()) {
                existingUser = userAccountRepository.findByUsername(username);
            }
            
            if (existingUser.isPresent()) {
                // Update existing user to ensure correct credentials
                UserAccount user = existingUser.get();
                user.setUsername(username);
                user.setEmail(email);
                user.setPassword(encodedPassword);
                user.setRoles(List.of(role));
                user.setEnabled(true);
                userAccountRepository.save(user);
                System.out.println("✅ Updated existing demo account: " + email);
            } else {
                // Create new user
                UserAccount newUser = new UserAccount();
                newUser.setId(sequenceGeneratorService.getSequenceNumber(UserAccount.class.getSimpleName()));
                newUser.setUsername(username);
                newUser.setEmail(email);
                newUser.setPassword(encodedPassword);
                newUser.setRoles(List.of(role));
                newUser.setEnabled(true);
                userAccountRepository.save(newUser);
                System.out.println("✅ Created new demo account: " + email);
            }
        }
        
        System.out.println("✅ Demo user accounts seeded/updated successfully.");
    }

    private void initializeAdditionalFinanceData(List<Account> accounts) {
        System.out.println("🧪 Adding extra finance dummy data...");
        Account cash = accounts.stream().filter(a -> a.getName().equals("Cash")).findFirst().orElse(null);
        Account supplies = accounts.stream().filter(a -> a.getName().equals("Office Supplies")).findFirst().orElse(null);
        Account travel = accounts.stream().filter(a -> a.getName().equals("Travel Expense")).findFirst().orElse(null);
        Account utilities = accounts.stream().filter(a -> a.getName().equals("Utilities Expense")).findFirst().orElse(null);
        Account serviceRevenue = accounts.stream().filter(a -> a.getName().equals("Service Revenue")).findFirst().orElse(null);
        Account consultingRevenue = accounts.stream().filter(a -> a.getName().equals("Consulting Revenue")).findFirst().orElse(null);
        Account receivable = accounts.stream().filter(a -> a.getName().equals("Accounts Receivable")).findFirst().orElse(null);
        if (cash == null || supplies == null || travel == null || utilities == null) {
            System.out.println("⚠️ Skipping extra expenses due to missing accounts");
            return;
        }
        // Extra random expenses
        for (int i = 0; i < 10; i++) {
            Expense e = new Expense();
            e.setTitle("Misc Expense " + (i + 1));
            e.setDescription("Auto-generated expense record #" + (i + 1));
            e.setAmount(new BigDecimal(5000 + random.nextInt(45000)).setScale(2));
            e.setExpenseDate(LocalDate.now().minusDays(random.nextInt(40)));
            e.setCategory(i % 3 == 0 ? ExpenseCategory.TRAVEL : i % 3 == 1 ? ExpenseCategory.OTHER : ExpenseCategory.UTILITIES);
            e.setStatus(random.nextBoolean() ? PaymentStatus.PAID : PaymentStatus.PENDING);
            e.setDebitAccount((i % 3 == 0) ? travel : (i % 3 == 1 ? supplies : utilities));
            e.setCreditAccount(cash);
            e.setId(sequenceGeneratorService.getSequenceNumber(Expense.class.getSimpleName()));
            expenseRepository.save(e);
        }
        // Extra random invoices
        if (receivable != null && serviceRevenue != null && consultingRevenue != null) {
            for (int i = 0; i < 5; i++) {
                Invoice inv = new Invoice();
                inv.setInvoiceNumber("INV-EXTRA-" + (100 + i));
                inv.setInvoiceDate(LocalDate.now().minusDays(5 + i));
                inv.setCustomerName("Client " + (char)('A' + i));
                inv.setCustomerEmail("client" + i + "@example.com");
                BigDecimal amount = new BigDecimal(200000 + random.nextInt(800000));
                BigDecimal tax = amount.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
                inv.setAmount(amount);
                inv.setTaxAmount(tax);
                inv.setTotalAmount(amount.add(tax));
                inv.setStatus(i % 2 == 0 ? PaymentStatus.PAID : PaymentStatus.PENDING);
                inv.setDueDate(LocalDate.now().plusDays(20 + i));
                inv.setDebitAccount(receivable);
                inv.setCreditAccount(i % 2 == 0 ? serviceRevenue : consultingRevenue);
                inv.setId(sequenceGeneratorService.getSequenceNumber(Invoice.class.getSimpleName()));
                invoiceRepository.save(inv);
            }
        }
        System.out.println("✅ Extra finance dummy data added");
    }

    // Helper methods for generating random data
    private AttendanceStatus generateAttendanceStatus() {
        int rand = random.nextInt(100);
        if (rand < 85) return AttendanceStatus.PRESENT;
        if (rand < 92) return AttendanceStatus.LATE;
        if (rand < 96) return AttendanceStatus.WORK_FROM_HOME;
        if (rand < 98) return AttendanceStatus.HALF_DAY;
        return AttendanceStatus.ABSENT;
    }

    private String generateAttendanceNotes(AttendanceStatus status) {
        switch (status) {
            case LATE: return "Traffic delay";
            case ABSENT: return "Sick leave";
            case WORK_FROM_HOME: return "Remote work";
            case HALF_DAY: return "Personal appointment";
            default: return "";
        }
    }

    private Long getTotalLeaveAllowance(LeaveType leaveType, EmployeeType employeeType) {
        switch (leaveType) {
            case ANNUAL: return employeeType == EmployeeType.FULL_TIME ? 21L : 15L;
            case SICK: return 10L;
            case PERSONAL: return 5L;
            case MATERNITY: return 90L;
            case PATERNITY: return 15L;
            case EMERGENCY: return 3L;
            case COMPENSATORY: return 5L;
            default: return 0L;
        }
    }

    private LeaveStatus generateLeaveStatus() {
        int rand = random.nextInt(100);
        if (rand < 60) return LeaveStatus.APPROVED;
        if (rand < 80) return LeaveStatus.PENDING;
        return LeaveStatus.REJECTED;
    }

    private String generateLeaveReason(LeaveType leaveType) {
        switch (leaveType) {
            case ANNUAL: return "Family vacation";
            case SICK: return "Medical treatment required";
            case PERSONAL: return "Personal matters to attend";
            case MATERNITY: return "Maternity leave";
            case PATERNITY: return "Paternity leave";
            case EMERGENCY: return "Family emergency";
            case COMPENSATORY: return "Compensatory leave for overtime";
            default: return "Standard leave request";
        }
    }

    private String generateRandomAddress() {
        String[] streets = {"Gulshan Avenue", "Banani Road", "Dhanmondi Road", "Uttara Main Road", "Mirpur Road", "Baridhara Road", "Bashundhara Road", "Mohakhali Road", "Tejgaon Road", "Farmgate Road"};
        String[] areas = {"Gulshan", "Banani", "Dhanmondi", "Uttara", "Mirpur", "Baridhara", "Bashundhara", "Mohakhali", "Tejgaon", "Farmgate"};
        String[] cities = {"Dhaka", "Chittagong", "Sylhet", "Rajshahi", "Khulna", "Barisal", "Rangpur", "Comilla", "Gazipur", "Narayanganj"};
        String[] divisions = {"Dhaka Division", "Chittagong Division", "Sylhet Division", "Rajshahi Division", "Khulna Division", "Barisal Division", "Rangpur Division", "Chittagong Division", "Dhaka Division", "Dhaka Division"};
        
        int number = 10 + random.nextInt(990);
        String street = streets[random.nextInt(streets.length)];
        String area = areas[random.nextInt(areas.length)];
        String city = cities[random.nextInt(cities.length)];
        String division = divisions[random.nextInt(divisions.length)];
        int pincode = 1000 + random.nextInt(8999);
        
        return number + ", " + street + ", " + area + ", " + city + ", " + division + " - " + pincode;
    }

    private Expense createExpense(String title, String description, BigDecimal amount, ExpenseCategory category,
                                Account debitAccount, Account creditAccount, PaymentStatus status, LocalDate date) {
        Expense expense = new Expense();
        expense.setTitle(title);
        expense.setDescription(description);
        expense.setAmount(amount);
        expense.setExpenseDate(date);
        expense.setCategory(category);
        expense.setStatus(status);
        expense.setDebitAccount(debitAccount);
        expense.setCreditAccount(creditAccount);
        return expense;
    }

    private Invoice createInvoice(String invoiceNumber, String customerName, String customerEmail,
                                BigDecimal amount, BigDecimal taxAmount, Account debitAccount, Account creditAccount,
                                PaymentStatus status, LocalDate invoiceDate, LocalDate dueDate) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setInvoiceDate(invoiceDate);
        invoice.setCustomerName(customerName);
        invoice.setCustomerEmail(customerEmail);
        invoice.setAmount(amount);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotalAmount(amount.add(taxAmount));
        invoice.setStatus(status);
        invoice.setDueDate(dueDate);
        invoice.setDebitAccount(debitAccount);
        invoice.setCreditAccount(creditAccount);
        return invoice;
    }

    private void printDataSummary() {
        System.out.println("\n📊 DATA INITIALIZATION SUMMARY");
        System.out.println("=====================================");
        System.out.println("🏢 Departments: " + departmentRepository.count());
        System.out.println("💼 Positions: " + positionRepository.count());
        System.out.println("👥 Employees: " + employeeRepository.count());
        System.out.println("📅 Attendance Records: " + attendanceRepository.count());
        System.out.println("🏖️ Leave Balances: " + leaveBalanceRepository.count());
        System.out.println("📋 Leave Requests: " + leaveRequestRepository.count());
        System.out.println("🏦 Chart of Accounts: " + accountRepository.count());
        System.out.println("💸 Expenses: " + expenseRepository.count());
        System.out.println("📄 Invoices: " + invoiceRepository.count());
        System.out.println("💰 Payroll Entries: " + payrollEntryRepository.count());
        System.out.println("=====================================");
        System.out.println("🎉 Your ERP system is now ready with sample data!");
        System.out.println("🌐 MongoDB Database: mydatabase");
        System.out.println("🔗 Connection established successfully!");
    }
}

