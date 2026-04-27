import React, { useCallback, useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  AlertCircle,
  ArrowLeft,
  BadgeDollarSign,
  BarChart3,
  Building2,
  CheckCircle2,
  CreditCard,
  ExternalLink,
  Pencil,
  Plus,
  RefreshCw,
  Save,
  Search,
  Trash2,
  Users,
  WalletCards,
  X
} from "lucide-react";
import "./index.css";

const root = document.getElementById("bank-root");
const boot = {
  contextPath: window.bankApp?.contextPath || "",
  page: root?.dataset.page || "dashboard",
  entityId: root?.dataset.entityId || "",
  kind: root?.dataset.kind || "",
  clientId: root?.dataset.clientId || ""
};

function url(path) {
  return `${boot.contextPath}${path}`;
}

function pageUrl(path) {
  window.location.href = url(path);
}

async function apiGet(path, params = {}) {
  const target = new URL(url(path), window.location.origin);
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== "") {
      target.searchParams.set(key, value);
    }
  });
  const response = await fetch(target);
  return response.json();
}

async function apiPost(path, body = {}) {
  const payload = new URLSearchParams();
  Object.entries(body).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      payload.set(key, value);
    }
  });
  const response = await fetch(url(path), {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
    body: payload
  });
  return response.json();
}

function useApi(loader, deps = []) {
  const [state, setState] = useState({ loading: true, data: null, error: null });
  const reload = useCallback(async () => {
    setState((current) => ({ ...current, loading: true, error: null }));
    try {
      const data = await loader();
      setState({ loading: false, data, error: data.success === false ? data.message : null });
    } catch (error) {
      setState({ loading: false, data: null, error: error.message });
    }
  }, deps);

  useEffect(() => {
    reload();
  }, [reload]);

  return { ...state, reload };
}

function money(value) {
  const number = Number(value || 0);
  return new Intl.NumberFormat("ru-RU", { maximumFractionDigits: 2 }).format(number);
}

function dateTime(value) {
  if (!value) {
    return "—";
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return String(value);
  }
  return parsed.toLocaleString("ru-RU", { dateStyle: "medium", timeStyle: "short" });
}

function todayPlus(days) {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return date.toISOString().slice(0, 10);
}

const titles = {
  dashboard: "Сводка банка",
  reports: "Отчеты",
  departments: "Отделения",
  "department-detail": "Карточка отделения",
  "department-form": boot.entityId ? "Редактирование отделения" : "Новое отделение",
  clients: "Клиенты",
  "client-detail": "Карточка клиента",
  "client-form": boot.entityId ? "Редактирование клиента" : "Новый клиент",
  accounts: "Счета",
  "account-detail": "Карточка счета",
  "account-form": "Открытие счета",
  "operation-form": boot.kind === "debit" ? "Списание средств" : "Начисление средств"
};

const nav = [
  { path: "/", page: "dashboard", label: "Сводка", icon: BarChart3 },
  { path: "/departments", page: "departments", label: "Отделения", icon: Building2 },
  { path: "/clients", page: "clients", label: "Клиенты", icon: Users },
  { path: "/accounts", page: "accounts", label: "Счета", icon: WalletCards },
  { path: "/reports", page: "reports", label: "Отчеты", icon: BadgeDollarSign }
];

function App() {
  const CurrentPage = pages[boot.page] || DashboardPage;
  return (
    <div className="min-h-screen">
      <aside className="fixed inset-y-0 left-0 hidden w-64 border-r border-zinc-200 bg-white lg:block">
        <div className="flex h-16 items-center gap-3 border-b border-zinc-200 px-6">
          <div className="flex h-10 w-10 items-center justify-center rounded bg-teal-700 text-white">
            <CreditCard size={22} />
          </div>
          <div>
            <div className="text-sm font-semibold uppercase tracking-wide text-zinc-500">Bank Web</div>
            <div className="text-base font-semibold text-zinc-950">Клиенты и счета</div>
          </div>
        </div>
        <nav className="p-3">
          {nav.map((item) => (
            <NavLink key={item.path} item={item} />
          ))}
        </nav>
      </aside>

      <div className="lg:pl-64">
        <header className="sticky top-0 z-10 border-b border-zinc-200 bg-white/95 backdrop-blur">
          <div className="flex min-h-16 flex-col gap-3 px-4 py-3 sm:flex-row sm:items-center sm:justify-between lg:px-8">
            <div>
              <h1 className="text-xl font-semibold text-zinc-950">{titles[boot.page] || "Банк"}</h1>
              <div className="mt-1 flex flex-wrap gap-2 lg:hidden">
                {nav.map((item) => (
                  <a
                    key={item.path}
                    href={url(item.path)}
                    className="rounded border border-zinc-200 px-2.5 py-1.5 text-sm text-zinc-700"
                  >
                    {item.label}
                  </a>
                ))}
              </div>
            </div>
            <div className="text-sm text-zinc-500">Оператор банка</div>
          </div>
        </header>
        <main className="px-4 py-6 lg:px-8">
          <CurrentPage />
        </main>
      </div>
    </div>
  );
}

function NavLink({ item }) {
  const Icon = item.icon;
  const active = boot.page === item.page || (item.page !== "dashboard" && boot.page.startsWith(item.page.slice(0, -1)));
  return (
    <a
      href={url(item.path)}
      data-testid={`nav-${item.page}`}
      className={`mb-1 flex items-center gap-3 rounded px-3 py-2.5 text-sm font-medium focus-ring ${
        active ? "bg-teal-50 text-teal-800" : "text-zinc-700 hover:bg-zinc-100"
      }`}
    >
      <Icon size={18} />
      {item.label}
    </a>
  );
}

function Toolbar({ children }) {
  return <div className="mb-5 flex flex-col gap-3 md:flex-row md:items-end md:justify-between">{children}</div>;
}

function Button({ children, icon: Icon, tone = "primary", className = "", ...props }) {
  const tones = {
    primary: "border-teal-700 bg-teal-700 text-white hover:bg-teal-800",
    secondary: "border-zinc-300 bg-white text-zinc-800 hover:bg-zinc-100",
    danger: "border-rose-700 bg-rose-700 text-white hover:bg-rose-800",
    quiet: "border-transparent bg-transparent text-zinc-700 hover:bg-zinc-100"
  };
  return (
    <button
      type="button"
      className={`inline-flex min-h-10 items-center justify-center gap-2 rounded border px-3.5 py-2 text-sm font-medium focus-ring disabled:cursor-not-allowed disabled:opacity-55 ${tones[tone]} ${className}`}
      {...props}
    >
      {Icon ? <Icon size={17} /> : null}
      {children}
    </button>
  );
}

function LinkButton({ href, children, icon, tone = "primary", ...props }) {
  return (
    <a href={url(href)} {...props}>
      <Button as="span" icon={icon} tone={tone}>
        {children}
      </Button>
    </a>
  );
}

function Notice({ notice }) {
  if (!notice) {
    return null;
  }
  const success = notice.success !== false;
  return (
    <div
      data-testid="notice"
      className={`mb-5 flex items-start gap-3 rounded border px-4 py-3 text-sm ${
        success ? "border-emerald-200 bg-emerald-50 text-emerald-900" : "border-rose-200 bg-rose-50 text-rose-900"
      }`}
    >
      {success ? <CheckCircle2 size={18} /> : <AlertCircle size={18} />}
      <span>{notice.message}</span>
    </div>
  );
}

function Loading() {
  return (
    <div className="flex min-h-48 items-center justify-center text-zinc-500">
      <RefreshCw className="mr-2 animate-spin" size={18} />
      Загрузка
    </div>
  );
}

function Empty({ children = "Нет данных" }) {
  return <div className="rounded border border-dashed border-zinc-300 bg-white px-5 py-10 text-center text-zinc-500">{children}</div>;
}

function Field({ label, name, value, onChange, type = "text", required = false, testId, ...props }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-medium text-zinc-700">{label}</span>
      <input
        name={name}
        value={value ?? ""}
        onChange={(event) => onChange(name, event.target.value)}
        type={type}
        required={required}
        data-testid={testId}
        className="focus-ring h-10 w-full rounded border border-zinc-300 bg-white px-3 text-sm text-zinc-950"
        {...props}
      />
    </label>
  );
}

function SelectField({ label, name, value, onChange, children, testId }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-medium text-zinc-700">{label}</span>
      <select
        name={name}
        value={value ?? ""}
        onChange={(event) => onChange(name, event.target.value)}
        data-testid={testId}
        className="focus-ring h-10 w-full rounded border border-zinc-300 bg-white px-3 text-sm text-zinc-950"
      >
        {children}
      </select>
    </label>
  );
}

function TextArea({ label, name, value, onChange, testId }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-medium text-zinc-700">{label}</span>
      <textarea
        name={name}
        value={value ?? ""}
        onChange={(event) => onChange(name, event.target.value)}
        data-testid={testId}
        rows={4}
        className="focus-ring w-full resize-y rounded border border-zinc-300 bg-white px-3 py-2 text-sm text-zinc-950"
      />
    </label>
  );
}

function StatusBadge({ children, status }) {
  const cls =
    status === "active"
      ? "bg-emerald-100 text-emerald-800"
      : status === "closed"
        ? "bg-zinc-100 text-zinc-700"
        : "bg-amber-100 text-amber-800";
  return <span className={`inline-flex rounded px-2 py-1 text-xs font-medium ${cls}`}>{children}</span>;
}

function Table({ columns, rows, getKey, empty }) {
  if (!rows?.length) {
    return <Empty>{empty}</Empty>;
  }
  return (
    <div className="overflow-x-auto rounded border border-zinc-200 bg-white">
      <table className="min-w-full divide-y divide-zinc-200 text-left text-sm">
        <thead className="bg-zinc-50 text-xs uppercase text-zinc-500">
          <tr>
            {columns.map((column) => (
              <th key={column.key} className="whitespace-nowrap px-4 py-3 font-semibold">
                {column.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-zinc-100">
          {rows.map((row) => (
            <tr key={getKey(row)} data-testid={`row-${getKey(row)}`} className="hover:bg-zinc-50">
              {columns.map((column) => (
                <td key={column.key} className="whitespace-nowrap px-4 py-3 align-top">
                  {column.render ? column.render(row) : row[column.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function Stat({ label, value, icon: Icon }) {
  return (
    <div className="rounded border border-zinc-200 bg-white p-4">
      <div className="mb-3 flex items-center justify-between text-zinc-500">
        <span className="text-sm">{label}</span>
        {Icon ? <Icon size={18} /> : null}
      </div>
      <div className="text-2xl font-semibold text-zinc-950">{value}</div>
    </div>
  );
}

function DashboardPage() {
  const { loading, data, error } = useApi(() => apiGet("/api/dashboard"), []);
  if (loading) return <Loading />;
  if (error) return <Notice notice={{ success: false, message: error }} />;
  const totals = data.totals || {};
  return (
    <div>
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <Stat label="Клиенты" value={totals.clientCount} icon={Users} />
        <Stat label="Счета" value={totals.accountCount} icon={WalletCards} />
        <Stat label="Общий баланс" value={money(totals.totalBalance)} icon={BadgeDollarSign} />
        <Stat label="Активные счета" value={totals.activeAccounts} icon={CheckCircle2} />
      </div>
      <section className="mt-7">
        <h2 className="mb-3 text-base font-semibold text-zinc-950">Отделения</h2>
        <DepartmentsTable rows={data.departments || []} />
      </section>
      <section className="mt-7">
        <h2 className="mb-3 text-base font-semibold text-zinc-950">Типы счетов</h2>
        <Table
          rows={data.accountTypes || []}
          getKey={(row) => row.accountType}
          empty="Нет счетов"
          columns={[
            { key: "accountTypeLabel", label: "Тип" },
            { key: "count", label: "Количество" },
            { key: "totalBalance", label: "Баланс", render: (row) => money(row.totalBalance) }
          ]}
        />
      </section>
    </div>
  );
}

function ReportsPage() {
  const { loading, data, error } = useApi(() => apiGet("/api/dashboard"), []);
  if (loading) return <Loading />;
  if (error) return <Notice notice={{ success: false, message: error }} />;
  const totals = data.totals || {};
  return (
    <div>
      <div className="grid gap-4 md:grid-cols-3">
        <Stat label="Выдано кредитов" value={money(totals.creditDebt)} icon={CreditCard} />
        <Stat label="Привлечено вкладов" value={money(totals.depositAmount)} icon={BadgeDollarSign} />
        <Stat label="Суммарный остаток" value={money(totals.totalBalance)} icon={WalletCards} />
      </div>
      <section className="mt-7">
        <h2 className="mb-3 text-base font-semibold text-zinc-950">Сводка по отделениям</h2>
        <DepartmentsTable rows={data.departments || []} />
      </section>
    </div>
  );
}

function DepartmentsPage() {
  const [filters, setFilters] = useState({ search: "" });
  const [query, setQuery] = useState({});
  const { loading, data, error, reload } = useApi(() => apiGet("/api/departments", query), [JSON.stringify(query)]);
  const change = (name, value) => setFilters((current) => ({ ...current, [name]: value }));
  return (
    <div>
      <Toolbar>
        <form
          className="flex w-full max-w-xl gap-2"
          onSubmit={(event) => {
            event.preventDefault();
            setQuery(filters);
          }}
        >
          <Field label="Поиск" name="search" value={filters.search} onChange={change} testId="departments-search" />
          <div className="pt-6">
            <Button type="submit" icon={Search} tone="secondary">
              Найти
            </Button>
          </div>
        </form>
        <Button icon={Plus} onClick={() => pageUrl("/departments/new")} data-testid="add-department">
          Добавить
        </Button>
      </Toolbar>
      {loading ? <Loading /> : error ? <Notice notice={{ success: false, message: error }} /> : <DepartmentsTable rows={data.departments || []} reload={reload} />}
    </div>
  );
}

function DepartmentsTable({ rows }) {
  return (
    <Table
      rows={rows}
      getKey={(row) => `department-${row.id}`}
      empty="Отделения не найдены"
      columns={[
        {
          key: "name",
          label: "Название",
          render: (row) => (
            <a className="font-medium text-teal-800 hover:text-teal-950" href={url(`/departments/${row.id}`)}>
              {row.name}
            </a>
          )
        },
        { key: "address", label: "Адрес", render: (row) => row.address || "—" },
        { key: "phoneNumber", label: "Телефон", render: (row) => row.phoneNumber || "—" },
        { key: "clientCount", label: "Клиенты" },
        { key: "totalBalance", label: "Баланс", render: (row) => money(row.totalBalance) }
      ]}
    />
  );
}

function DepartmentDetailPage() {
  const id = boot.entityId;
  const [notice, setNotice] = useState(null);
  const { loading, data, error, reload } = useApi(() => apiGet(`/api/departments/${id}`), [id]);
  async function remove() {
    const result = await apiPost(`/api/departments/${id}/delete`);
    setNotice(result);
    if (result.success) {
      pageUrl("/departments");
    }
  }
  if (loading) return <Loading />;
  if (error) return <Notice notice={{ success: false, message: error }} />;
  const department = data.department;
  return (
    <div>
      <Notice notice={notice} />
      <Toolbar>
        <Button icon={ArrowLeft} tone="secondary" onClick={() => pageUrl("/departments")}>
          Назад
        </Button>
        <div className="flex gap-2">
          <Button icon={Pencil} tone="secondary" onClick={() => pageUrl(`/departments/${id}/edit`)} data-testid="edit-department">
            Редактировать
          </Button>
          <Button icon={Trash2} tone="danger" onClick={remove} data-testid="delete-department">
            Удалить
          </Button>
        </div>
      </Toolbar>
      <section className="rounded border border-zinc-200 bg-white p-5">
        <h2 data-testid="department-title" className="text-xl font-semibold text-zinc-950">{department.name}</h2>
        <dl className="mt-4 grid gap-4 md:grid-cols-4">
          <Info label="Адрес" value={department.address} />
          <Info label="Телефон" value={department.phoneNumber} />
          <Info label="Email" value={department.email} />
          <Info label="Клиенты" value={department.clientCount} />
        </dl>
      </section>
      <section className="mt-7">
        <h2 className="mb-3 text-base font-semibold text-zinc-950">Клиенты отделения</h2>
        <ClientsTable rows={data.clients || []} />
      </section>
      <section className="mt-7">
        <h2 className="mb-3 text-base font-semibold text-zinc-950">Счета отделения</h2>
        <AccountsTable rows={data.accounts || []} />
      </section>
    </div>
  );
}

function DepartmentFormPage() {
  const id = boot.entityId;
  const [form, setForm] = useState({ id, name: "", address: "", phoneNumber: "", email: "", longitude: "", latitude: "" });
  const [notice, setNotice] = useState(null);
  const change = (name, value) => setForm((current) => ({ ...current, [name]: value }));
  const { loading } = useApi(async () => {
    if (!id) return { success: true };
    const data = await apiGet(`/api/departments/${id}`);
    if (data.success) {
      setForm((current) => ({ ...current, ...data.department }));
    } else {
      setNotice(data);
    }
    return data;
  }, [id]);

  async function save(event) {
    event.preventDefault();
    const result = await apiPost("/api/departments/save", form);
    setNotice(result);
    if (result.success) {
      pageUrl(`/departments/${result.id}`);
    }
  }

  if (loading && id) return <Loading />;
  return (
    <FormFrame onSubmit={save} notice={notice} back="/departments">
      <div className="grid gap-4 md:grid-cols-2">
        <Field label="Название" name="name" value={form.name} onChange={change} required testId="department-name-input" />
        <Field label="Email" name="email" value={form.email} onChange={change} type="email" />
        <Field label="Адрес" name="address" value={form.address} onChange={change} />
        <Field label="Телефон" name="phoneNumber" value={form.phoneNumber} onChange={change} />
        <Field label="Долгота" name="longitude" value={form.longitude ?? ""} onChange={change} />
        <Field label="Широта" name="latitude" value={form.latitude ?? ""} onChange={change} />
      </div>
      <FormActions saveTestId="save-department" />
    </FormFrame>
  );
}

function ClientsPage() {
  const [filters, setFilters] = useState({ departmentId: "", type: "", search: "" });
  const [query, setQuery] = useState({});
  const { loading, data, error } = useApi(() => apiGet("/api/clients", query), [JSON.stringify(query)]);
  const change = (name, value) => setFilters((current) => ({ ...current, [name]: value }));
  return (
    <div>
      <Toolbar>
        <form
          className="grid w-full gap-3 md:grid-cols-4"
          onSubmit={(event) => {
            event.preventDefault();
            setQuery(filters);
          }}
        >
          <SelectField label="Отделение" name="departmentId" value={filters.departmentId} onChange={change} testId="clients-department-filter">
            <option value="">Все</option>
            {(data?.departments || []).map((department) => (
              <option key={department.id} value={department.id}>{department.name}</option>
            ))}
          </SelectField>
          <SelectField label="Тип" name="type" value={filters.type} onChange={change} testId="clients-type-filter">
            <option value="">Все</option>
            <option value="NATURAL_PERSON">Физическое лицо</option>
            <option value="LEGAL_ENTITY">Юридическое лицо</option>
          </SelectField>
          <Field label="Поиск" name="search" value={filters.search} onChange={change} testId="clients-search" />
          <div className="pt-6">
            <Button type="submit" icon={Search} tone="secondary">Найти</Button>
          </div>
        </form>
        <Button icon={Plus} onClick={() => pageUrl("/clients/new")} data-testid="add-client">Добавить</Button>
      </Toolbar>
      {loading ? <Loading /> : error ? <Notice notice={{ success: false, message: error }} /> : <ClientsTable rows={data.clients || []} />}
    </div>
  );
}

function ClientsTable({ rows }) {
  return (
    <Table
      rows={rows}
      getKey={(row) => `client-${row.id}`}
      empty="Клиенты не найдены"
      columns={[
        {
          key: "name",
          label: "Клиент",
          render: (row) => (
            <a className="font-medium text-teal-800 hover:text-teal-950" href={url(`/clients/${row.id}`)}>
              {row.name}
            </a>
          )
        },
        { key: "typeLabel", label: "Тип" },
        { key: "departmentName", label: "Отделение" },
        { key: "phone", label: "Телефон", render: (row) => row.phone || "—" },
        { key: "accountCount", label: "Счета" },
        { key: "totalBalance", label: "Баланс", render: (row) => money(row.totalBalance) }
      ]}
    />
  );
}

function ClientDetailPage() {
  const id = boot.entityId;
  const [notice, setNotice] = useState(null);
  const { loading, data, error } = useApi(() => apiGet(`/api/clients/${id}`), [id]);
  async function remove() {
    const result = await apiPost(`/api/clients/${id}/delete`);
    setNotice(result);
    if (result.success) {
      pageUrl("/clients");
    }
  }
  if (loading) return <Loading />;
  if (error) return <Notice notice={{ success: false, message: error }} />;
  const client = data.client;
  return (
    <div>
      <Notice notice={notice} />
      <Toolbar>
        <Button icon={ArrowLeft} tone="secondary" onClick={() => pageUrl("/clients")}>Назад</Button>
        <div className="flex flex-wrap gap-2">
          <Button icon={Plus} onClick={() => pageUrl(`/accounts/new?clientId=${id}`)} data-testid="open-account">Открыть счет</Button>
          <Button icon={Pencil} tone="secondary" onClick={() => pageUrl(`/clients/${id}/edit`)} data-testid="edit-client">Редактировать</Button>
          <Button icon={Trash2} tone="danger" onClick={remove} data-testid="delete-client">Удалить</Button>
        </div>
      </Toolbar>
      <section className="rounded border border-zinc-200 bg-white p-5">
        <h2 data-testid="client-title" className="text-xl font-semibold text-zinc-950">{client.name}</h2>
        <dl className="mt-4 grid gap-4 md:grid-cols-4">
          <Info label="Тип" value={client.typeLabel} />
          <Info label="Отделение" value={client.departmentName} />
          <Info label="Телефон" value={client.phone} />
          <Info label="Баланс" value={money(client.totalBalance)} />
          <Info label="Паспорт" value={client.passport} />
          <Info label="СНИЛС" value={client.snils} />
          <Info label="Адрес" value={client.address} />
        </dl>
      </section>
      <section className="mt-7">
        <h2 className="mb-3 text-base font-semibold text-zinc-950">Счета клиента</h2>
        <AccountsTable rows={data.accounts || []} />
      </section>
      <section className="mt-7">
        <h2 className="mb-3 text-base font-semibold text-zinc-950">История операций</h2>
        <OperationsTable rows={data.operations || []} />
      </section>
    </div>
  );
}

function ClientFormPage() {
  const id = boot.entityId;
  const [form, setForm] = useState({
    id,
    departmentId: "",
    type: "NATURAL_PERSON",
    surname: "",
    secondName: "",
    passport: "",
    snils: "",
    phone: "",
    address: "",
    linkToPhoto: ""
  });
  const [notice, setNotice] = useState(null);
  const { loading, data } = useApi(async () => {
    const reference = await apiGet("/api/reference");
    if (id) {
      const detail = await apiGet(`/api/clients/${id}`);
      if (detail.success) {
        const client = detail.client;
        setForm((current) => ({
          ...current,
          ...client,
          type: client.type === "legal entity" ? "LEGAL_ENTITY" : "NATURAL_PERSON"
        }));
      } else {
        setNotice(detail);
      }
    }
    return reference;
  }, [id]);
  const change = (name, value) => setForm((current) => ({ ...current, [name]: value }));

  async function save(event) {
    event.preventDefault();
    const result = await apiPost("/api/clients/save", form);
    setNotice(result);
    if (result.success) {
      pageUrl(`/clients/${result.id}`);
    }
  }

  if (loading) return <Loading />;
  return (
    <FormFrame onSubmit={save} notice={notice} back="/clients">
      <div className="grid gap-4 md:grid-cols-2">
        <SelectField label="Тип клиента" name="type" value={form.type} onChange={change} testId="client-type-select">
          <option value="NATURAL_PERSON">Физическое лицо</option>
          <option value="LEGAL_ENTITY">Юридическое лицо</option>
        </SelectField>
        <SelectField label="Отделение" name="departmentId" value={form.departmentId} onChange={change} testId="client-department-select">
          <option value="">Выберите</option>
          {(data?.departments || []).map((department) => (
            <option key={department.id} value={department.id}>{department.name}</option>
          ))}
        </SelectField>
        <Field label={form.type === "LEGAL_ENTITY" ? "Название" : "Фамилия"} name="surname" value={form.surname} onChange={change} required testId="client-name-input" />
        <Field label={form.type === "LEGAL_ENTITY" ? "Контактное лицо" : "Имя"} name="secondName" value={form.secondName || ""} onChange={change} />
        <Field label="Паспорт" name="passport" value={form.passport || ""} onChange={change} />
        <Field label="СНИЛС" name="snils" value={form.snils || ""} onChange={change} />
        <Field label="Телефон" name="phone" value={form.phone || ""} onChange={change} />
        <Field label="Фото URL" name="linkToPhoto" value={form.linkToPhoto || ""} onChange={change} />
        <div className="md:col-span-2">
          <TextArea label="Адрес" name="address" value={form.address || ""} onChange={change} />
        </div>
      </div>
      <FormActions saveTestId="save-client" />
    </FormFrame>
  );
}

function AccountsPage() {
  const [filters, setFilters] = useState({ clientId: "", accountType: "", status: "", createdFrom: "", createdTo: "" });
  const [query, setQuery] = useState({});
  const { loading, data, error } = useApi(() => apiGet("/api/accounts", query), [JSON.stringify(query)]);
  const change = (name, value) => setFilters((current) => ({ ...current, [name]: value }));
  return (
    <div>
      <Toolbar>
        <form
          className="grid w-full gap-3 md:grid-cols-6"
          onSubmit={(event) => {
            event.preventDefault();
            setQuery(filters);
          }}
        >
          <SelectField label="Клиент" name="clientId" value={filters.clientId} onChange={change} testId="accounts-client-filter">
            <option value="">Все</option>
            {(data?.clients || []).map((client) => (
              <option key={client.id} value={client.id}>{client.name}</option>
            ))}
          </SelectField>
          <SelectField label="Тип" name="accountType" value={filters.accountType} onChange={change} testId="accounts-type-filter">
            <option value="">Все</option>
            <option value="credit">Кредит</option>
            <option value="deposit">Вклад</option>
            <option value="saving">Накопительный</option>
          </SelectField>
          <SelectField label="Статус" name="status" value={filters.status} onChange={change} testId="accounts-status-filter">
            <option value="">Все</option>
            <option value="active">Открыт</option>
            <option value="closed">Закрыт</option>
            <option value="suspended">Приостановлен</option>
          </SelectField>
          <Field label="Открыт с" name="createdFrom" value={filters.createdFrom} onChange={change} type="date" />
          <Field label="Открыт по" name="createdTo" value={filters.createdTo} onChange={change} type="date" />
          <div className="pt-6">
            <Button type="submit" icon={Search} tone="secondary">Найти</Button>
          </div>
        </form>
        <Button icon={Plus} onClick={() => pageUrl("/accounts/new")} data-testid="add-account">Открыть</Button>
      </Toolbar>
      {loading ? <Loading /> : error ? <Notice notice={{ success: false, message: error }} /> : <AccountsTable rows={data.accounts || []} />}
    </div>
  );
}

function AccountsTable({ rows }) {
  return (
    <Table
      rows={rows}
      getKey={(row) => `account-${row.id}`}
      empty="Счета не найдены"
      columns={[
        {
          key: "specialNumber",
          label: "Номер",
          render: (row) => (
            <a className="font-medium text-teal-800 hover:text-teal-950" href={url(`/accounts/${row.id}`)}>
              {row.specialNumber}
            </a>
          )
        },
        { key: "clientName", label: "Клиент" },
        { key: "departmentName", label: "Отделение" },
        { key: "accountTypeLabel", label: "Тип" },
        { key: "statusLabel", label: "Статус", render: (row) => <StatusBadge status={row.status}>{row.statusLabel}</StatusBadge> },
        { key: "points", label: "Баланс", render: (row) => `${money(row.points)} ${row.currency}` },
        { key: "createdAt", label: "Открыт", render: (row) => dateTime(row.createdAt) }
      ]}
    />
  );
}

function AccountDetailPage() {
  const id = boot.entityId;
  const [notice, setNotice] = useState(null);
  const { loading, data, error, reload } = useApi(() => apiGet(`/api/accounts/${id}`), [id]);
  async function close() {
    const result = await apiPost(`/api/accounts/${id}/close`);
    setNotice(result);
    if (result.success) {
      reload();
    }
  }
  if (loading) return <Loading />;
  if (error) return <Notice notice={{ success: false, message: error }} />;
  const account = data.account;
  const active = account.status === "active";
  return (
    <div>
      <Notice notice={notice} />
      <Toolbar>
        <Button icon={ArrowLeft} tone="secondary" onClick={() => pageUrl("/accounts")}>Назад</Button>
        <div className="flex flex-wrap gap-2">
          {active ? (
            <>
              <Button icon={Plus} onClick={() => pageUrl(`/accounts/${id}/operation?kind=credit`)} data-testid="credit-account">Начислить</Button>
              <Button icon={BadgeDollarSign} tone="secondary" onClick={() => pageUrl(`/accounts/${id}/operation?kind=debit`)} data-testid="debit-account">Списать</Button>
              <Button icon={X} tone="danger" onClick={close} data-testid="close-account">Закрыть</Button>
            </>
          ) : null}
        </div>
      </Toolbar>
      <section className="rounded border border-zinc-200 bg-white p-5">
        <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <h2 data-testid="account-title" className="text-xl font-semibold text-zinc-950">{account.specialNumber}</h2>
          <StatusBadge status={account.status}>{account.statusLabel}</StatusBadge>
        </div>
        <dl className="mt-4 grid gap-4 md:grid-cols-4">
          <Info label="Клиент" value={account.clientName} />
          <Info label="Отделение" value={account.departmentName} />
          <Info label="Тип" value={account.accountTypeLabel} />
          <Info label="Баланс" value={`${money(account.points)} ${account.currency}`} />
          <Info label="Открыт" value={dateTime(account.createdAt)} />
          <Info label="Закрыт" value={dateTime(account.closedAt)} />
          {Object.entries(account.details || {}).map(([key, value]) => (
            <Info key={key} label={detailLabel(key)} value={String(value)} />
          ))}
        </dl>
      </section>
      <section className="mt-7">
        <h2 className="mb-3 text-base font-semibold text-zinc-950">История операций</h2>
        <OperationsTable rows={data.operations || []} />
      </section>
    </div>
  );
}

function AccountFormPage() {
  const [form, setForm] = useState({
    clientId: boot.clientId || "",
    currencyId: "",
    accountType: "saving",
    specialNumber: `ACC-${Date.now()}`,
    interestRate: "5.0",
    maxLimit: "100000",
    maxCredit: "100000",
    currentDebt: "0",
    initialAmount: "0",
    endDate: todayPlus(180),
    automaticRenewal: "false",
    paymentMethod: "manual"
  });
  const [notice, setNotice] = useState(null);
  const { loading, data } = useApi(async () => {
    const reference = await apiGet("/api/reference");
    const firstCurrency = reference.currencies?.[0]?.id;
    if (firstCurrency) {
      setForm((current) => ({ ...current, currencyId: current.currencyId || firstCurrency }));
    }
    return reference;
  }, []);
  const change = (name, value) => setForm((current) => ({ ...current, [name]: value }));

  async function save(event) {
    event.preventDefault();
    const result = await apiPost("/api/accounts/save", form);
    setNotice(result);
    if (result.success) {
      pageUrl(`/accounts/${result.id}`);
    }
  }

  if (loading) return <Loading />;
  return (
    <FormFrame onSubmit={save} notice={notice} back="/accounts">
      <div className="grid gap-4 md:grid-cols-2">
        <SelectField label="Клиент" name="clientId" value={form.clientId} onChange={change} testId="account-client-select">
          <option value="">Выберите</option>
          {(data?.clients || []).map((client) => (
            <option key={client.id} value={client.id}>{client.name}</option>
          ))}
        </SelectField>
        <SelectField label="Валюта" name="currencyId" value={form.currencyId} onChange={change} testId="account-currency-select">
          <option value="">Выберите</option>
          {(data?.currencies || []).map((currency) => (
            <option key={currency.id} value={currency.id}>{currency.name}</option>
          ))}
        </SelectField>
        <SelectField label="Тип счета" name="accountType" value={form.accountType} onChange={change} testId="account-type-select">
          <option value="saving">Накопительный</option>
          <option value="deposit">Вклад</option>
          <option value="credit">Кредит</option>
        </SelectField>
        <Field label="Номер счета" name="specialNumber" value={form.specialNumber} onChange={change} required testId="account-number-input" />
        {form.accountType === "saving" ? (
          <>
            <Field label="Процентная ставка" name="interestRate" value={form.interestRate} onChange={change} testId="saving-rate-input" />
            <Field label="Максимальный лимит" name="maxLimit" value={form.maxLimit} onChange={change} testId="saving-limit-input" />
          </>
        ) : null}
        {form.accountType === "deposit" ? (
          <>
            <Field label="Начальная сумма" name="initialAmount" value={form.initialAmount} onChange={change} testId="deposit-amount-input" />
            <Field label="Дата окончания" name="endDate" value={form.endDate} onChange={change} type="date" testId="deposit-end-input" />
            <SelectField label="Выплаты" name="paymentMethod" value={form.paymentMethod} onChange={change}>
              <option value="manual">Ручной</option>
              <option value="auto">Авто</option>
            </SelectField>
            <SelectField label="Автопролонгация" name="automaticRenewal" value={form.automaticRenewal} onChange={change}>
              <option value="false">Нет</option>
              <option value="true">Да</option>
            </SelectField>
          </>
        ) : null}
        {form.accountType === "credit" ? (
          <>
            <Field label="Кредитный лимит" name="maxCredit" value={form.maxCredit} onChange={change} testId="credit-limit-input" />
            <Field label="Текущая задолженность" name="currentDebt" value={form.currentDebt} onChange={change} />
            <Field label="Процентная ставка" name="interestRate" value={form.interestRate} onChange={change} />
            <SelectField label="Погашение" name="paymentMethod" value={form.paymentMethod} onChange={change}>
              <option value="manual">Ручной</option>
              <option value="auto">Авто</option>
            </SelectField>
          </>
        ) : null}
      </div>
      <FormActions saveTestId="save-account" />
    </FormFrame>
  );
}

function OperationFormPage() {
  const id = boot.entityId;
  const [form, setForm] = useState({ kind: boot.kind || "credit", amount: "", description: "" });
  const [notice, setNotice] = useState(null);
  const { loading, data, error } = useApi(() => apiGet(`/api/accounts/${id}`), [id]);
  const change = (name, value) => setForm((current) => ({ ...current, [name]: value }));

  async function save(event) {
    event.preventDefault();
    const result = await apiPost(`/api/accounts/${id}/operation`, form);
    setNotice(result);
    if (result.success) {
      window.setTimeout(() => pageUrl(`/accounts/${id}`), 250);
    }
  }

  if (loading) return <Loading />;
  if (error) return <Notice notice={{ success: false, message: error }} />;
  const account = data.account;
  return (
    <FormFrame onSubmit={save} notice={notice} back={`/accounts/${id}`}>
      <section className="mb-5 rounded border border-zinc-200 bg-zinc-50 p-4">
        <div className="text-sm text-zinc-500">Счет</div>
        <div className="mt-1 font-semibold text-zinc-950">{account.specialNumber} · {money(account.points)} {account.currency}</div>
      </section>
      <div className="grid gap-4 md:grid-cols-2">
        <SelectField label="Операция" name="kind" value={form.kind} onChange={change} testId="operation-kind-select">
          <option value="credit">Начисление</option>
          <option value="debit">Списание</option>
        </SelectField>
        <Field label="Сумма" name="amount" value={form.amount} onChange={change} required testId="operation-amount-input" />
        <div className="md:col-span-2">
          <TextArea label="Назначение" name="description" value={form.description} onChange={change} testId="operation-description-input" />
        </div>
      </div>
      <FormActions saveTestId="save-operation" />
    </FormFrame>
  );
}

function OperationsTable({ rows }) {
  return (
    <Table
      rows={rows}
      getKey={(row) => `operation-${row.id}`}
      empty="Операций нет"
      columns={[
        { key: "createdAt", label: "Дата", render: (row) => dateTime(row.createdAt) },
        {
          key: "specialNumber",
          label: "Счет",
          render: (row) => (
            <a className="font-medium text-teal-800 hover:text-teal-950" href={url(`/accounts/${row.accountId}`)}>
              {row.specialNumber}
            </a>
          )
        },
        { key: "kindLabel", label: "Тип" },
        { key: "amount", label: "Сумма", render: (row) => money(row.amount) },
        { key: "description", label: "Назначение", render: (row) => row.description || "—" }
      ]}
    />
  );
}

function FormFrame({ children, onSubmit, notice, back }) {
  return (
    <div>
      <Toolbar>
        <Button icon={ArrowLeft} tone="secondary" onClick={() => pageUrl(back)}>Назад</Button>
      </Toolbar>
      <Notice notice={notice} />
      <form noValidate onSubmit={onSubmit} className="rounded border border-zinc-200 bg-white p-5">
        {children}
      </form>
    </div>
  );
}

function FormActions({ saveTestId }) {
  return (
    <div className="mt-6 flex flex-wrap gap-2 border-t border-zinc-200 pt-5">
      <Button type="submit" icon={Save} data-testid={saveTestId}>Сохранить</Button>
      <Button icon={X} tone="secondary" onClick={() => window.history.back()}>Отменить</Button>
    </div>
  );
}

function Info({ label, value }) {
  return (
    <div>
      <dt className="text-sm text-zinc-500">{label}</dt>
      <dd className="mt-1 break-words text-sm font-medium text-zinc-950">{value || "—"}</dd>
    </div>
  );
}

function detailLabel(key) {
  return {
    maxCredit: "Кредитный лимит",
    currentDebt: "Задолженность",
    interestRate: "Ставка",
    paymentMethod: "Метод",
    paymentMethodLabel: "Метод",
    initialAmount: "Сумма открытия",
    endDate: "Дата окончания",
    automaticRenewal: "Автопролонгация",
    maxLimit: "Максимальный лимит"
  }[key] || key;
}

const pages = {
  dashboard: DashboardPage,
  reports: ReportsPage,
  departments: DepartmentsPage,
  "department-detail": DepartmentDetailPage,
  "department-form": DepartmentFormPage,
  clients: ClientsPage,
  "client-detail": ClientDetailPage,
  "client-form": ClientFormPage,
  accounts: AccountsPage,
  "account-detail": AccountDetailPage,
  "account-form": AccountFormPage,
  "operation-form": OperationFormPage
};

createRoot(root).render(<App />);
