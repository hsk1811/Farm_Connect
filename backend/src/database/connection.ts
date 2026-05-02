import 'dotenv/config';
import { Pool, PoolClient } from 'pg';

// ─── Connection Pool ──────────────────────────────────────────────────────────

const pool = new Pool({
    host: process.env.PGHOST,
    port: Number(process.env.PGPORT) || 5432,
    user: process.env.PGUSER,
    password: process.env.PGPASSWORD,
    database: process.env.PGDATABASE,
    ssl: { rejectUnauthorized: false },
    max: 10,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 5000,
});

pool.on('error', (err) => {
    console.error('Unexpected error on idle PostgreSQL client', err);
});

// ─── Param Converter: ? → $1, $2, ... ────────────────────────────────────────

function convertParams(sql: string): string {
    let index = 0;
    return sql.replace(/\?/g, () => `$${++index}`);
}

// ─── Prepared Statement Wrapper ───────────────────────────────────────────────

function prepare(sql: string) {
    const pgSql = convertParams(sql);

    return {
        async run(...params: any[]): Promise<{ changes: number; lastInsertRowid: number }> {
            const flatParams = params.length === 1 && Array.isArray(params[0]) ? params[0] : params;
            // Add RETURNING id if it's an INSERT and no RETURNING clause yet
            let finalSql = pgSql;
            if (/^\s*INSERT/i.test(pgSql) && !/RETURNING/i.test(pgSql)) {
                finalSql = pgSql + ' RETURNING id';
            }
            const result = await pool.query(finalSql, flatParams);
            const lastId = result.rows && result.rows.length > 0 && result.rows[0].id
                ? Number(result.rows[0].id)
                : 0;
            return {
                changes: result.rowCount ?? 0,
                lastInsertRowid: lastId,
            };
        },

        async get(...params: any[]): Promise<any> {
            const flatParams = params.length === 1 && Array.isArray(params[0]) ? params[0] : params;
            const result = await pool.query(pgSql, flatParams);
            return result.rows[0] ?? undefined;
        },

        async all(...params: any[]): Promise<any[]> {
            const flatParams = params.length === 1 && Array.isArray(params[0]) ? params[0] : params;
            const result = await pool.query(pgSql, flatParams);
            return result.rows;
        },
    };
}

// ─── exec: for DDL / raw SQL with no params ───────────────────────────────────

async function exec(sql: string): Promise<void> {
    await pool.query(sql);
}

// ─── Raw query for advanced use ───────────────────────────────────────────────

async function rawQuery(sql: string, params: any[] = []): Promise<any[]> {
    const result = await pool.query(sql, params);
    return result.rows;
}

// ─── Initialise (just test the connection) ────────────────────────────────────

export async function initDatabase(): Promise<void> {
    const client: PoolClient = await pool.connect();
    client.release();
    console.log('✅ Connected to Supabase PostgreSQL');
}

// ─── Public db object ─────────────────────────────────────────────────────────

const db = { prepare, exec, rawQuery, pool };

export function getDatabase() { return db; }
export default db;
