import { jsPDF } from 'jspdf';
import { Observation, Project } from '../types';

export function generateNcrPdf(obs: Observation, project: Project): jsPDF {
  const doc = new jsPDF({
    orientation: 'portrait',
    unit: 'mm',
    format: 'a4'
  });

  const pageWidth = 210;
  const pageHeight = 297;
  const margin = 14;
  const contentWidth = pageWidth - margin * 2;

  // Header Banner - Navy #0F294A
  doc.setFillColor(15, 41, 74);
  doc.rect(margin, margin, contentWidth, 24, 'F');

  // Title
  doc.setTextColor(255, 255, 255);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(14);
  doc.text('SnagOps - NON-CONFORMANCE REPORT (NCR)', margin + 6, margin + 10);

  doc.setFont('helvetica', 'normal');
  doc.setFontSize(8.5);
  doc.text(`Project: ${project.projectName} (${project.projectCode})`, margin + 6, margin + 16);
  doc.text(`Client / Contractor: ${project.clientContractorName}`, margin + 6, margin + 21);

  // Status & Priority Badge on top right
  const badgeX = pageWidth - margin - 40;
  doc.setFillColor(220, 38, 38); // Red
  doc.roundedRect(badgeX, margin + 5, 34, 14, 2, 2, 'F');
  doc.setTextColor(255, 255, 255);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(9);
  doc.text('NCR ISSUED', badgeX + 17, margin + 11, { align: 'center' });
  doc.setFontSize(7.5);
  doc.text(`PRIORITY: ${obs.priority}`, badgeX + 17, margin + 16, { align: 'center' });

  let y = margin + 28;

  // Metadata Grid Box
  doc.setFillColor(248, 250, 252); // light slate
  doc.setDrawColor(203, 213, 225); // slate-300
  doc.rect(margin, y, contentWidth, 38, 'FD');

  doc.setTextColor(30, 41, 59);
  doc.setFontSize(8.5);

  const col1 = margin + 4;
  const col2 = margin + 92;

  const dateLoggedStr = new Date(obs.dateLogged).toLocaleDateString('en-GB');
  const targetDateStr = new Date(obs.targetDate).toLocaleDateString('en-GB');

  // Row 1
  doc.setFont('helvetica', 'bold');
  doc.text('NCR Number:', col1, y + 6);
  doc.setFont('helvetica', 'normal');
  doc.text(obs.entryNumber, col1 + 25, y + 6);

  doc.setFont('helvetica', 'bold');
  doc.text('Date Logged:', col2, y + 6);
  doc.setFont('helvetica', 'normal');
  doc.text(dateLoggedStr, col2 + 26, y + 6);

  // Row 2
  doc.setFont('helvetica', 'bold');
  doc.text('Logged By:', col1, y + 13);
  doc.setFont('helvetica', 'normal');
  doc.text(`${obs.loggedBy} (${obs.loggedByInitials})`, col1 + 25, y + 13);

  doc.setFont('helvetica', 'bold');
  doc.text('Target Closure:', col2, y + 13);
  doc.setFont('helvetica', 'normal');
  doc.text(targetDateStr, col2 + 26, y + 13);

  // Row 3
  doc.setFont('helvetica', 'bold');
  doc.text('Location:', col1, y + 20);
  doc.setFont('helvetica', 'normal');
  doc.text(`${obs.block} > ${obs.floor} > ${obs.specificLocation}`, col1 + 25, y + 20);

  doc.setFont('helvetica', 'bold');
  doc.text('Current Status:', col2, y + 20);
  doc.setFont('helvetica', 'bold');
  if (obs.status === 'RECTIFIED') {
    doc.setTextColor(22, 163, 74);
    doc.text('RECTIFIED', col2 + 26, y + 20);
  } else {
    doc.setTextColor(220, 38, 38);
    doc.text('OPEN / PENDING', col2 + 26, y + 20);
  }
  doc.setTextColor(30, 41, 59);

  // Row 4
  doc.setFont('helvetica', 'bold');
  doc.text('Responsible Vendor:', col1, y + 27);
  doc.setFont('helvetica', 'normal');
  doc.text(`${obs.vendorName} (${obs.trade})`, col1 + 34, y + 27);

  doc.setFont('helvetica', 'bold');
  doc.text('Record UUID:', col2, y + 27);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(7);
  doc.text(obs.recordUuid, col2 + 26, y + 27);
  doc.setFontSize(8.5);

  y += 42;

  // Section 1: Non-Conformance Description
  doc.setFillColor(241, 245, 249);
  doc.rect(margin, y, contentWidth, 6, 'F');
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(9);
  doc.setTextColor(15, 23, 42);
  doc.text('1. DESCRIPTION OF NON-CONFORMANCE', margin + 3, y + 4.5);
  y += 8;

  doc.setFont('helvetica', 'normal');
  doc.setFontSize(8.5);
  doc.setTextColor(51, 65, 85);
  const descLines = doc.splitTextToSize(obs.issueDescription || 'No description provided.', contentWidth - 6);
  doc.text(descLines, margin + 3, y);
  y += Math.max(10, descLines.length * 4.5) + 3;

  // Section 2: Root Cause Analysis
  doc.setFillColor(241, 245, 249);
  doc.rect(margin, y, contentWidth, 6, 'F');
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(9);
  doc.setTextColor(15, 23, 42);
  doc.text('2. ROOT CAUSE ANALYSIS', margin + 3, y + 4.5);
  y += 8;

  doc.setFont('helvetica', 'normal');
  doc.setFontSize(8.5);
  doc.setTextColor(51, 65, 85);
  const rootLines = doc.splitTextToSize(obs.rootCause || 'Under investigation by contractor QA/QC team.', contentWidth - 6);
  doc.text(rootLines, margin + 3, y);
  y += Math.max(8, rootLines.length * 4.5) + 3;

  // Section 3: Corrective & Preventive Action
  doc.setFillColor(241, 245, 249);
  doc.rect(margin, y, contentWidth, 6, 'F');
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(9);
  doc.setTextColor(15, 23, 42);
  doc.text('3. CORRECTIVE & PREVENTIVE ACTION REQUIRED', margin + 3, y + 4.5);
  y += 8;

  doc.setFont('helvetica', 'normal');
  doc.setFontSize(8.5);
  doc.setTextColor(51, 65, 85);
  const capaLines = doc.splitTextToSize(obs.correctiveAction || 'Rectify defect per approved specifications and resubmit for QA re-inspection.', contentWidth - 6);
  doc.text(capaLines, margin + 3, y);
  y += Math.max(8, capaLines.length * 4.5) + 4;

  // Section 4: Photographic Evidence
  doc.setFillColor(241, 245, 249);
  doc.rect(margin, y, contentWidth, 6, 'F');
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(9);
  doc.setTextColor(15, 23, 42);
  doc.text('4. PHOTOGRAPHIC EVIDENCE (DEFECT & RECTIFICATION)', margin + 3, y + 4.5);
  y += 9;

  const photoBoxWidth = (contentWidth - 6) / 2;
  const photoBoxHeight = 55;

  // Defect Photo Box
  doc.setDrawColor(203, 213, 225);
  doc.rect(margin, y, photoBoxWidth, photoBoxHeight);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(7.5);
  doc.setTextColor(100, 116, 139);
  doc.text('INITIAL DEFECT PHOTO', margin + 3, y + 5);

  if (obs.beforePhotoData) {
    try {
      doc.addImage(obs.beforePhotoData, 'JPEG', margin + 2, y + 7, photoBoxWidth - 4, photoBoxHeight - 9);
    } catch {
      doc.text('(Photo Attached in Outbox / Drive)', margin + 4, y + 25);
    }
  } else {
    doc.text('(No Initial Photo Captured)', margin + 4, y + 25);
  }

  // Rectification Photo Box
  const rectX = margin + photoBoxWidth + 6;
  doc.rect(rectX, y, photoBoxWidth, photoBoxHeight);
  doc.text('RECTIFICATION / CLOSURE PHOTO', rectX + 3, y + 5);

  if (obs.afterPhotoData) {
    try {
      doc.addImage(obs.afterPhotoData, 'JPEG', rectX + 2, y + 7, photoBoxWidth - 4, photoBoxHeight - 9);
    } catch {
      doc.text('(Photo Attached in Outbox / Drive)', rectX + 4, y + 25);
    }
  } else {
    doc.text('(Pending Contractor Rectification)', rectX + 4, y + 25);
  }

  y += photoBoxHeight + 8;

  // Section 5: Sign-off & Verification Boxes
  doc.setFillColor(241, 245, 249);
  doc.rect(margin, y, contentWidth, 6, 'F');
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(9);
  doc.setTextColor(15, 23, 42);
  doc.text('5. SIGN-OFF & VERIFICATION', margin + 3, y + 4.5);
  y += 8;

  const signBoxWidth = (contentWidth - 6) / 2;
  const signBoxHeight = 22;

  // Sign Box 1: Issued By
  doc.setDrawColor(203, 213, 225);
  doc.rect(margin, y, signBoxWidth, signBoxHeight);
  doc.setFontSize(7.5);
  doc.setTextColor(100, 116, 139);
  doc.text('ISSUED BY (PMC / QA ENGINEER):', margin + 3, y + 5);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(15, 23, 42);
  doc.text(`Name: ${obs.loggedBy}`, margin + 3, y + 10);
  doc.text(`Initials: ${obs.loggedByInitials}  |  Date: ${dateLoggedStr}`, margin + 3, y + 15);
  doc.text('Signature: __________________________', margin + 3, y + 20);

  // Sign Box 2: Rectified & Accepted By
  doc.rect(rectX, y, signBoxWidth, signBoxHeight);
  doc.setFontSize(7.5);
  doc.setTextColor(100, 116, 139);
  doc.text('RECTIFICATION VERIFIED & CLOSED BY:', rectX + 3, y + 5);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(15, 23, 42);
  const rectDateStr = obs.rectifiedDate ? new Date(obs.rectifiedDate).toLocaleDateString('en-GB') : 'Pending';
  doc.text(`Status: ${obs.status === 'RECTIFIED' ? 'Accepted & Closed' : 'Pending Verification'}`, rectX + 3, y + 10);
  doc.text(`Closure Date: ${rectDateStr}`, rectX + 3, y + 15);
  doc.text('Signature: __________________________', rectX + 3, y + 20);

  // Footer
  const footerY = pageHeight - 8;
  doc.setFontSize(7);
  doc.setTextColor(148, 163, 184);
  doc.text(
    `SnagOps Quality Management System • Document Sync UUID: ${obs.recordUuid} • Auto-Generated Document`,
    pageWidth / 2,
    footerY,
    { align: 'center' }
  );

  return doc;
}
