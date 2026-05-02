import { v4 as uuidv4 } from 'uuid';

export function generateContractNumber(): string {
    const date = new Date();
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const unique = uuidv4().split('-')[0].toUpperCase();

    return `FC-${year}${month}${day}-${unique}`;
}

export function calculateExpiryDate(days: number): string {
    const date = new Date();
    date.setDate(date.getDate() + days);
    return date.toISOString();
}

export function formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR'
    }).format(amount);
}

export function calculateTotalValue(quantity: number, price: number): number {
    return Math.round(quantity * price * 100) / 100;
}

export function isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

export function sanitizeInput(input: string): string {
    return input.trim().replace(/[<>]/g, '');
}

export function paginate(page: number, limit: number, total: number) {
    return {
        page,
        limit,
        total,
        pages: Math.ceil(total / limit)
    };
}
