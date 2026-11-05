export interface AppInfo {
  appName: string;
  authenticated: boolean;
}

export interface SetupStatus {
  setupComplete: boolean;
  appName: string;
  logoUrl: string | null;
}

export interface StageCell {
  entrantId: number;
  millis: number | null;
  kind: 'VALID' | 'PENALTY' | 'RETIRED' | 'EC' | 'NONE';
  best: boolean;
  record: boolean;
  bestGapMs: number | null;
  prevGapMs: number | null;
  prevPosition: number | null;
  prevName: string | null;
  provisional: boolean;
}

export interface StageRow {
  position: number;
  entrantId: number;
  name: string;
  car: string;
  millis: number | null;
  kind: 'VALID' | 'PENALTY';
  best: boolean;
  record: boolean;
  bestGapMs: number | null;
  prevGapMs: number | null;
  prevPosition: number | null;
  prevName: string | null;
}

export interface Stage {
  id: number;
  code: string;
  name: string;
  type: 'TC' | 'SHAKEDOWN';
  position: number;
  status: 'PENDING' | 'LIVE' | 'CLOSED' | 'VOID';
  bestMs: number | null;
  bestEntrantId: number | null;
  recordMs: number | null;
  recordHolder: string | null;
  recordCar: string | null;
  recordRally: string | null;
  recordDate: string | null;
  cells: StageCell[];
  rows: StageRow[];
  missingNames: string[];
}

export interface GeneralRow {
  position: number | null;
  entrantId: number;
  name: string;
  car: string;
  status: 'RANKED' | 'EC' | 'RETIRED';
  totalMs: number | null;
  partialMs: number | null;
  difLeaderMs: number | null;
  difPrevMs: number | null;
  prevPosition: number | null;
  prevName: string | null;
  leader: boolean;
}

export interface Entrant {
  id: number;
  name: string;
  car: string;
  position: number;
  retired: boolean;
  retiredAtPosition: number | null;
}

export interface RallyView {
  id: number;
  name: string;
  date: string;
  status: string;
  shakedownEnabled: boolean;
  shakedownName: string | null;
  penaltyMode: 'WORST_PLUS' | 'FIXED';
  penaltyValueMs: number;
  inviteToken: string;
  version: number;
  stages: Stage[];
  general: GeneralRow[];
  entrants: Entrant[];
  currentStageId: number | null;
  currentStageCode: string;
  currentStageStatus: string;
  missingNames: string[];
}

export interface RallySummary {
  id: number;
  name: string;
  date: string;
  status: string;
  leader: string | null;
  winner: string | null;
  stagesTotal: number;
  stagesClosed: number;
  active: boolean;
  inviteToken: string;
}

export interface RecordEntry {
  position: number;
  pilot: string;
  car: string;
  millis: number;
  rally: string;
  date: string;
}

export interface RecordView {
  name: string;
  key: string;
  type: string;
  recordMs: number;
  holder: string;
  car: string;
  rally: string;
  date: string;
  top: RecordEntry[];
}

export interface Dictionaries {
  stages: string[];
  cars: string[];
  pilots: string[];
}

export interface RallyConfigRequest {
  name: string;
  stages: { name: string }[];
  entrants: { name: string; car: string }[];
  shakedownEnabled: boolean;
  shakedownName: string;
  penaltyMode: 'WORST_PLUS' | 'FIXED';
  penaltyValueMs: number;
}

export interface ResultRequest {
  kind: 'VALID' | 'PENALTY' | 'CLEAR';
  millis?: number | null;
}

export interface EditTarget {
  stage: Stage;
  row: GeneralRow;
  cell: StageCell;
}
