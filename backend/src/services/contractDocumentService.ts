import crypto from 'crypto';
import PDFDocument from 'pdfkit';
import path from 'path';
import db from '../database';
import { Contract } from '../types';
import { supabaseClient } from '../config/supabaseClient';

export interface ContractData {
    id: number;
    contractNumber: string;
    farmerName: string;
    farmerId: number;
    buyerName: string;
    buyerId: number;
    cropType: string;
    variety?: string;
    quantity: number;
    unit: string;
    agreedPrice: number;
    totalValue: number;
    deliveryAddress?: string;
    qualityGrade?: string;
    paymentTerms?: string;
    transportResponsibility?: string;
    additionalTerms?: string;
    createdAt: string;
}

/**
 * Generate a SHA-256 hash of contract terms
 * This hash ensures the contract cannot be tampered with
 */
export function generateContractHash(data: ContractData): string {
    const contractString = JSON.stringify({
        contractNumber: data.contractNumber,
        farmerId: data.farmerId,
        buyerId: data.buyerId,
        cropType: data.cropType,
        variety: data.variety,
        quantity: data.quantity,
        unit: data.unit,
        agreedPrice: data.agreedPrice,
        totalValue: data.totalValue,
        deliveryAddress: data.deliveryAddress,
        qualityGrade: data.qualityGrade,
        paymentTerms: data.paymentTerms,
        transportResponsibility: data.transportResponsibility,
        additionalTerms: data.additionalTerms,
        createdAt: data.createdAt
    });

    return crypto.createHash('sha256').update(contractString).digest('hex');
}

/**
 * Verify a contract's hash matches its data
 */
export function verifyContractHash(data: ContractData, storedHash: string): boolean {
    const calculatedHash = generateContractHash(data);
    return calculatedHash === storedHash;
}

/**
 * Generate a professional PDF contract directly into a Buffer
 */
export async function generateContractPDF(data: ContractData, hash: string): Promise<string> {
    return new Promise((resolve, reject) => {
        const filename = `contract_${data.contractNumber}.pdf`;
        const filepath = `contracts/${filename}`;

        const doc = new PDFDocument({ size: 'A4', margin: 50 });
        const chunks: Buffer[] = [];

        doc.on('data', (chunk) => {
            chunks.push(chunk);
        });

        doc.on('end', async () => {
            try {
                const pdfBuffer = Buffer.concat(chunks);

                // Upload to Supabase Storage
                const { error } = await supabaseClient
                    .storage
                    .from('farmconnect-uploads')
                    .upload(filepath, pdfBuffer, {
                        contentType: 'application/pdf',
                        upsert: true
                    });

                if (error) {
                    console.error('Supabase PDF upload error:', error);
                    return reject(error);
                }

                // Get public URL
                const { data: publicUrlData } = supabaseClient
                    .storage
                    .from('farmconnect-uploads')
                    .getPublicUrl(filepath);

                resolve(publicUrlData.publicUrl);
            } catch (err) {
                reject(err);
            }
        });

        doc.on('error', (err) => {
            reject(err);
        });

        // Header
        doc.fontSize(24).font('Helvetica-Bold').text('FARMING CONTRACT', { align: 'center' });
        doc.moveDown(0.5);
        doc.fontSize(12).font('Helvetica').text('FarmConnect Platform', { align: 'center' });
        doc.moveDown(2);

        // Contract Number Box
        doc.rect(50, doc.y, 495, 40).stroke();
        doc.fontSize(14).font('Helvetica-Bold').text(`Contract Number: ${data.contractNumber}`, 60, doc.y + 12);
        doc.moveDown(3);

        // Date
        const contractDate = new Date(data.createdAt).toLocaleDateString('en-IN', {
            day: '2-digit',
            month: 'long',
            year: 'numeric'
        });
        doc.fontSize(11).font('Helvetica').text(`Date of Agreement: ${contractDate}`);
        doc.moveDown(2);

        // Parties Section
        doc.fontSize(14).font('Helvetica-Bold').text('PARTIES TO THIS AGREEMENT');
        doc.moveDown(0.5);
        doc.fontSize(11).font('Helvetica');
        doc.text(`SELLER (Farmer): ${data.farmerName}`, { indent: 20 });
        doc.text(`Party ID: ${data.farmerId}`, { indent: 20 });
        doc.moveDown();
        doc.text(`BUYER: ${data.buyerName}`, { indent: 20 });
        doc.text(`Party ID: ${data.buyerId}`, { indent: 20 });
        doc.moveDown(2);

        // Terms Section
        doc.fontSize(14).font('Helvetica-Bold').text('CONTRACT TERMS');
        doc.moveDown(0.5);

        // Draw table
        const tableTop = doc.y;
        const tableLeft = 50;
        const colWidth = 247;

        const terms = [
            ['Crop Type', data.cropType],
            ['Variety', data.variety || 'N/A'],
            ['Quantity', `${data.quantity} ${data.unit}`],
            ['Agreed Price', `₹${data.agreedPrice.toLocaleString('en-IN')} per ${data.unit}`],
            ['Total Value', `₹${data.totalValue.toLocaleString('en-IN')}`],
            ['Quality Grade', data.qualityGrade || 'Standard'],
            ['Payment Terms', data.paymentTerms || 'Upon Delivery'],
            ['Transport', data.transportResponsibility === 'buyer' ? 'Buyer Pick-up' : (data.transportResponsibility === 'farmer' ? 'Farmer Delivery' : 'Mutual Agreement')],
            ['Delivery Location', data.deliveryAddress || 'To be decided']
        ];

        let yPos = tableTop;
        terms.forEach(([label, value], index) => {
            doc.rect(tableLeft, yPos, colWidth, 25).stroke();
            doc.rect(tableLeft + colWidth, yPos, colWidth, 25).stroke();
            doc.fontSize(10).font('Helvetica-Bold').text(label, tableLeft + 10, yPos + 8);
            doc.font('Helvetica').text(String(value), tableLeft + colWidth + 10, yPos + 8);
            yPos += 25;
        });

        if (data.additionalTerms) {
            yPos += 15;
            doc.rect(tableLeft, yPos, 495, 40).stroke();
            doc.fontSize(10).font('Helvetica-Bold').text('Additional Terms:', tableLeft + 10, yPos + 8);
            doc.font('Helvetica').text(data.additionalTerms, tableLeft + 10, yPos + 22);
            yPos += 40;
        }

        doc.y = yPos + 20;
        doc.moveDown(2);

        // Terms and Conditions
        doc.fontSize(14).font('Helvetica-Bold').text('STANDARD TERMS AND CONDITIONS');
        doc.moveDown(0.5);
        doc.fontSize(10).font('Helvetica');
        doc.text('1. The Seller agrees to deliver the specified quantity and quality of produce.');
        doc.text('2. The Buyer agrees to pay the full amount upon satisfactory delivery.');
        doc.text('3. Both parties agree that this contract is binding and cannot be altered.');
        doc.text('4. Any disputes will be resolved through FarmConnect mediation and arbitration.');
        doc.text('5. Force majeure events shall be handled as per applicable laws of India.');
        doc.moveDown(2);

        // Digital Verification
        doc.rect(50, doc.y, 495, 80).stroke();
        doc.fontSize(12).font('Helvetica-Bold').text('DIGITAL VERIFICATION', 60, doc.y + 10);
        doc.moveDown(0.5);
        doc.fontSize(8).font('Courier');
        doc.text(`Contract Hash (SHA-256):`, 60);
        doc.text(hash, 60);
        doc.moveDown(0.5);
        doc.fontSize(9).font('Helvetica-Oblique');
        doc.text('This hash cryptographically ensures the contract has not been tampered with.', 60);

        doc.moveDown(4);

        // Signatures
        doc.fontSize(11).font('Helvetica');
        doc.text('_________________________', 80);
        doc.text(`Farmer: ${data.farmerName}`, 80);

        doc.text('_________________________', 350);
        doc.text(`Buyer: ${data.buyerName}`, 350);

        // Footer
        doc.fontSize(8).font('Helvetica').text(
            'Generated by FarmConnect Platform | This is a legally binding digital contract',
            50, 750, { align: 'center', width: 495 }
        );

        doc.end();
    });
}

/**
 * Store contract hash in database
 */
export async function storeContractHash(contractId: number, hash: string, pdfPath: string): Promise<void> {
    await (await (await db.prepare(`
        UPDATE contracts 
        SET contract_hash = $1, pdf_path = $2, updated_at = NOW() 
        WHERE id = $3
    `))).run(hash, pdfPath, contractId);
}

/**
 * Get contract with verification info
 */
export async function getContractWithVerification(contractId: number): Promise<{
    contract: any;
    isVerified: boolean;
    hash: string;
    pdfUrl: string;
} | null> {
    const contract = await (await (await db.prepare(`
        SELECT c.*,
            fp.full_name as farmer_name,
            bp.full_name as buyer_name
        FROM contracts c
        JOIN user_profiles fp ON c.farmer_id = fp.user_id
        JOIN user_profiles bp ON c.buyer_id = bp.user_id
        WHERE c.id = $1
    `))).get(contractId) as any;

    if (!contract) return null;

    const contractData: ContractData = {
        id: contract.id,
        contractNumber: contract.contract_number,
        farmerName: contract.farmer_name,
        farmerId: contract.farmer_id,
        buyerName: contract.buyer_name,
        buyerId: contract.buyer_id,
        cropType: contract.crop_type,
        variety: contract.variety,
        quantity: contract.quantity,
        unit: contract.unit,
        agreedPrice: contract.agreed_price,
        totalValue: contract.total_value,
        deliveryAddress: contract.delivery_address,
        qualityGrade: contract.quality_grade,
        paymentTerms: contract.payment_terms,
        transportResponsibility: contract.transport_responsibility,
        additionalTerms: contract.additional_terms,
        createdAt: contract.created_at
    };

    const isVerified = contract.contract_hash
        ? verifyContractHash(contractData, contract.contract_hash)
        : false;

    return {
        contract,
        isVerified,
        hash: contract.contract_hash || '',
        pdfUrl: contract.pdf_path || ''
    };
}

/**
 * Generate and store contract document
 */
export async function finalizeContract(contractId: number): Promise<{
    hash: string;
    pdfUrl: string;
    isVerified: boolean;
}> {
    const contract = await (await (await db.prepare(`
        SELECT c.*,
            fp.full_name as farmer_name,
            bp.full_name as buyer_name
        FROM contracts c
        JOIN user_profiles fp ON c.farmer_id = fp.user_id
        JOIN user_profiles bp ON c.buyer_id = bp.user_id
        WHERE c.id = $1
    `))).get(contractId) as any;

    if (!contract) {
        throw new Error('Contract not found');
    }

    const contractData: ContractData = {
        id: contract.id,
        contractNumber: contract.contract_number,
        farmerName: contract.farmer_name,
        farmerId: contract.farmer_id,
        buyerName: contract.buyer_name,
        buyerId: contract.buyer_id,
        cropType: contract.crop_type,
        variety: contract.variety,
        quantity: contract.quantity,
        unit: contract.unit,
        agreedPrice: contract.agreed_price,
        totalValue: contract.total_value,
        deliveryAddress: contract.delivery_address,
        qualityGrade: contract.quality_grade,
        paymentTerms: contract.payment_terms,
        transportResponsibility: contract.transport_responsibility,
        additionalTerms: contract.additional_terms,
        createdAt: contract.created_at
    };

    // Generate hash
    const hash = generateContractHash(contractData);

    // Generate PDF and upload directly to Supabase
    const pdfUrl = await generateContractPDF(contractData, hash);

    // Store in database
    await storeContractHash(contractId, hash, pdfUrl);

    return { hash, pdfUrl, isVerified: true };
}
