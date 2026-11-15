import ExcelJS from 'exceljs';
import saveAs from 'file-saver';
import { Observation, Project, CustomFieldDefinition } from '../types';

export async function exportWorkbook(
  project: Project,
  observations: Observation[],
  customFieldDefs: CustomFieldDefinition[] = []
): Promise<void> {
  const workbook = new ExcelJS.Workbook();
  workbook.creator = 'SnagOps';
  workbook.lastModifiedBy = 'SnagOps Quality Management System';
  workbook.created = new Date();
  workbook.modified = new Date();

  const normalObs = observations.filter(o => o.recordType === 'NORMAL_OBSERVATION');
  const ncrObs = observations.filter(o => o.recordType === 'NCR');

  // ==========================================
  // TAB 1: Management_Dashboard
  // ==========================================
  const dashSheet = workbook.addWorksheet('Management_Dashboard', {
    views: [{ showGridLines: true }]
  });

  // Title Row
  dashSheet.mergeCells('A1:F1');
  const titleCell = dashSheet.getCell('A1');
  titleCell.value = `SnagOps Quality Management Dashboard - ${project.projectName}`;
  titleCell.font = { name: 'Arial', size: 16, bold: true, color: { argb: 'FFFFFFFF' } };
  titleCell.fill = {
    type: 'pattern',
    pattern: 'solid',
    fgColor: { argb: 'FF0F294A' } // Navy
  };
  titleCell.alignment = { vertical: 'middle', horizontal: 'center' };
  dashSheet.getRow(1).height = 36;

  // Subtitle Row
  dashSheet.mergeCells('A2:F2');
  const subCell = dashSheet.getCell('A2');
  subCell.value = `Client/Contractor: ${project.clientContractorName}  |  Code: ${project.projectCode}  |  Generated: ${new Date().toLocaleDateString('en-GB')}`;
  subCell.font = { name: 'Arial', size: 9, italic: true, color: { argb: 'FF64748B' } };
  subCell.alignment = { vertical: 'middle', horizontal: 'center' };
  dashSheet.getRow(2).height = 20;

  // Section: Overall Metrics Header
  dashSheet.getCell('A4').value = 'KEY PERFORMANCE INDICATORS';
  dashSheet.getCell('A4').font = { name: 'Arial', size: 11, bold: true, color: { argb: 'FF0F294A' } };

  // KPI Cards
  const kpiLabels = ['Total Observations', 'Open / In Progress', 'Rectified / Closed', 'Overall Closure Rate'];
  const totalCount = observations.length;
  const openCount = observations.filter(o => o.status === 'OPEN').length;
  const rectifiedCount = observations.filter(o => o.status === 'RECTIFIED').length;
  const closureRate = totalCount > 0 ? (rectifiedCount / totalCount) : 0;

  const kpiValues = [
    totalCount,
    openCount,
    rectifiedCount,
    `${(closureRate * 100).toFixed(1)}%`
  ];

  kpiLabels.forEach((label, idx) => {
    const colLetter = String.fromCharCode(65 + idx); // A, B, C, D
    const labelCell = dashSheet.getCell(`${colLetter}5`);
    labelCell.value = label;
    labelCell.font = { name: 'Arial', size: 9, bold: true, color: { argb: 'FF475569' } };
    labelCell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFF1F5F9' } };
    labelCell.alignment = { vertical: 'middle', horizontal: 'center' };

    const valCell = dashSheet.getCell(`${colLetter}6`);
    valCell.value = kpiValues[idx];
    valCell.font = { name: 'Arial', size: 16, bold: true, color: { argb: 'FF0F294A' } };
    valCell.alignment = { vertical: 'middle', horizontal: 'center' };
    valCell.border = {
      bottom: { style: 'medium', color: { argb: 'FF0F294A' } }
    };
  });
  dashSheet.getRow(5).height = 22;
  dashSheet.getRow(6).height = 30;

  // Breakdown Section
  dashSheet.getCell('A8').value = 'RECORD TYPE BREAKDOWN';
  dashSheet.getCell('A8').font = { name: 'Arial', size: 10, bold: true, color: { argb: 'FF0F294A' } };

  dashSheet.getCell('A9').value = 'Record Type';
  dashSheet.getCell('B9').value = 'Total Logged';
  dashSheet.getCell('C9').value = 'Open';
  dashSheet.getCell('D9').value = 'Rectified';
  ['A9', 'B9', 'C9', 'D9'].forEach(cell => {
    dashSheet.getCell(cell).font = { name: 'Arial', size: 9, bold: true, color: { argb: 'FFFFFFFF' } };
    dashSheet.getCell(cell).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF1E293B' } };
  });

  // Normal Obs Row
  dashSheet.getCell('A10').value = 'Normal Observations';
  dashSheet.getCell('B10').value = normalObs.length;
  dashSheet.getCell('C10').value = normalObs.filter(o => o.status === 'OPEN').length;
  dashSheet.getCell('D10').value = normalObs.filter(o => o.status === 'RECTIFIED').length;

  // NCR Row
  dashSheet.getCell('A11').value = 'Non-Conformance Reports (NCR)';
  dashSheet.getCell('B11').value = ncrObs.length;
  dashSheet.getCell('C11').value = ncrObs.filter(o => o.status === 'OPEN').length;
  dashSheet.getCell('D11').value = ncrObs.filter(o => o.status === 'RECTIFIED').length;

  // Set Column Widths for Dashboard
  dashSheet.columns = [
    { width: 28 },
    { width: 22 },
    { width: 22 },
    { width: 22 },
    { width: 18 },
    { width: 18 }
  ];

  // ==========================================
  // TAB 2: Observations (Normal Observations)
  // ==========================================
  const obsSheet = workbook.addWorksheet('Observations', {
    views: [{ showGridLines: true }]
  });

  const baseHeaders = [
    'Entry ID',
    'Record UUID',
    'Type',
    'Date Logged',
    'Logged By',
    'Initials',
    'Block / Zone',
    'Floor / Level',
    'Specific Location',
    'Vendor',
    'Trade',
    'Priority',
    'Target Date',
    'Status',
    'Rectified Date',
    'Issue Description'
  ];

  const customFieldNames = customFieldDefs.map(cf => cf.name);
  const obsHeaders = [...baseHeaders, ...customFieldNames, 'Photo Status'];

  const obsHeaderRow = obsSheet.addRow(obsHeaders);
  obsHeaderRow.height = 26;
  obsHeaderRow.eachCell(cell => {
    cell.font = { name: 'Arial', size: 10, bold: true, color: { argb: 'FFFFFFFF' } };
    cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF0F294A' } };
    cell.alignment = { vertical: 'middle', horizontal: 'center' };
  });

  normalObs.forEach(obs => {
    const rowValues = [
      obs.entryNumber,
      obs.recordUuid,
      'Normal Observation',
      new Date(obs.dateLogged).toLocaleDateString('en-GB'),
      obs.loggedBy,
      obs.loggedByInitials,
      obs.block,
      obs.floor,
      obs.specificLocation,
      obs.vendorName,
      obs.trade,
      obs.priority,
      new Date(obs.targetDate).toLocaleDateString('en-GB'),
      obs.status,
      obs.rectifiedDate ? new Date(obs.rectifiedDate).toLocaleDateString('en-GB') : '',
      obs.issueDescription
    ];

    // Dynamic custom field values
    customFieldDefs.forEach(cf => {
      rowValues.push(obs.customFields[cf.id] || '');
    });

    // Photo Status
    const photoStatus = obs.beforePhotoData && obs.afterPhotoData
      ? 'Before & After'
      : obs.beforePhotoData
      ? 'Before Photo'
      : 'No Photo';
    rowValues.push(photoStatus);

    const row = obsSheet.addRow(rowValues);
    row.height = 20;

    // Status coloring
    const statusCell = row.getCell(14);
    if (obs.status === 'RECTIFIED') {
      statusCell.font = { color: { argb: 'FF16A34A' }, bold: true };
    } else {
      statusCell.font = { color: { argb: 'FFDC2626' }, bold: true };
    }
  });

  obsSheet.columns = obsHeaders.map(h => ({
    width: Math.max(15, h.length + 4)
  }));

  // ==========================================
  // TAB 3: NCR_Register (Non-Conformance Reports)
  // ==========================================
  const ncrSheet = workbook.addWorksheet('NCR_Register', {
    views: [{ showGridLines: true }]
  });

  const ncrHeaders = [
    'NCR Number',
    'Record UUID',
    'Date Logged',
    'Logged By',
    'Initials',
    'Block / Zone',
    'Floor / Level',
    'Specific Location',
    'Responsible Vendor',
    'Trade',
    'Priority',
    'Target Date',
    'Status',
    'Rectified Date',
    'Non-Conformance Description',
    'Root Cause Analysis',
    'Corrective Action Required',
    ...customFieldNames,
    'Photo Evidence'
  ];

  const ncrHeaderRow = ncrSheet.addRow(ncrHeaders);
  ncrHeaderRow.height = 26;
  ncrHeaderRow.eachCell(cell => {
    cell.font = { name: 'Arial', size: 10, bold: true, color: { argb: 'FFFFFFFF' } };
    cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFDC2626' } }; // Red header for NCR
    cell.alignment = { vertical: 'middle', horizontal: 'center' };
  });

  ncrObs.forEach(obs => {
    const rowValues = [
      obs.entryNumber,
      obs.recordUuid,
      new Date(obs.dateLogged).toLocaleDateString('en-GB'),
      obs.loggedBy,
      obs.loggedByInitials,
      obs.block,
      obs.floor,
      obs.specificLocation,
      obs.vendorName,
      obs.trade,
      obs.priority,
      new Date(obs.targetDate).toLocaleDateString('en-GB'),
      obs.status,
      obs.rectifiedDate ? new Date(obs.rectifiedDate).toLocaleDateString('en-GB') : '',
      obs.issueDescription,
      obs.rootCause || '',
      obs.correctiveAction || ''
    ];

    customFieldDefs.forEach(cf => {
      rowValues.push(obs.customFields[cf.id] || '');
    });

    const photoStatus = obs.beforePhotoData && obs.afterPhotoData
      ? 'Defect & Rectification'
      : obs.beforePhotoData
      ? 'Defect Photo'
      : 'No Photo';
    rowValues.push(photoStatus);

    const row = ncrSheet.addRow(rowValues);
    row.height = 20;

    const statusCell = row.getCell(13);
    if (obs.status === 'RECTIFIED') {
      statusCell.font = { color: { argb: 'FF16A34A' }, bold: true };
    } else {
      statusCell.font = { color: { argb: 'FFDC2626' }, bold: true };
    }
  });

  ncrSheet.columns = ncrHeaders.map(h => ({
    width: Math.max(16, h.length + 4)
  }));

  // Generate & trigger browser download
  const buffer = await workbook.xlsx.writeBuffer();
  const blob = new Blob([buffer], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  });
  const dateStr = new Date().toISOString().slice(0, 10);
  saveAs(blob, `SnagOps_${project.projectCode}_QualityReport_${dateStr}.xlsx`);
}
