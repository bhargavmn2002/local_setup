'use client';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Plus } from 'lucide-react';

export type LayoutTemplate = {
  id: string;
  name: string;
  description: string;
  width: number;
  height: number;
  orientation: 'LANDSCAPE' | 'PORTRAIT';
  sections: {
    name: string;
    order: number;
    x: number;
    y: number;
    width: number;
    height: number;
    type?: 'media' | 'text'; // New field to distinguish section types
  }[];
};

export const LAYOUT_TEMPLATES: LayoutTemplate[] = [
  {
    id: 'single-pane',
    name: 'Single Pane',
    description: 'Full screen single zone layout',
    width: 1920,
    height: 1080,
    orientation: 'LANDSCAPE',
    sections: [
      { name: 'Section 1', order: 0, x: 0, y: 0, width: 100, height: 100 },
    ],
  },
  {
    id: 'split-2-horiz',
    name: 'Split 2 Horizontal',
    description: 'Two equal horizontal sections',
    width: 1920,
    height: 1080,
    orientation: 'LANDSCAPE',
    sections: [
      { name: 'Section 1', order: 0, x: 0, y: 0, width: 50, height: 100 },
      { name: 'Section 2', order: 1, x: 50, y: 0, width: 50, height: 100 },
    ],
  },
  {
    id: 'split-2-vert',
    name: 'Split 2 Vertical',
    description: 'Two equal vertical sections',
    width: 1920,
    height: 1080,
    orientation: 'LANDSCAPE',
    sections: [
      { name: 'Section 1', order: 0, x: 0, y: 0, width: 100, height: 50 },
      { name: 'Section 2', order: 1, x: 0, y: 50, width: 100, height: 50 },
    ],
  },
  {
    id: 'split-3-horiz',
    name: 'Split 3 Horizontal',
    description: 'Three equal horizontal sections',
    width: 1920,
    height: 1080,
    orientation: 'LANDSCAPE',
    sections: [
      { name: 'Section 1', order: 0, x: 0, y: 0, width: 33.33, height: 100 },
      { name: 'Section 2', order: 1, x: 33.33, y: 0, width: 33.33, height: 100 },
      { name: 'Section 3', order: 2, x: 66.66, y: 0, width: 33.34, height: 100 },
    ],
  },
  {
    id: 'split-3-vert',
    name: 'Split 3 Vertical',
    description: 'Three equal vertical sections',
    width: 1920,
    height: 1080,
    orientation: 'LANDSCAPE',
    sections: [
      { name: 'Section 1', order: 0, x: 0, y: 0, width: 100, height: 33.33 },
      { name: 'Section 2', order: 1, x: 0, y: 33.33, width: 100, height: 33.33 },
      { name: 'Section 3', order: 2, x: 0, y: 66.66, width: 100, height: 33.34 },
    ],
  },
  {
    id: 'four-grid',
    name: 'Four Grid',
    description: 'Four equal sections in a 2x2 grid',
    width: 1920,
    height: 1080,
    orientation: 'LANDSCAPE',
    sections: [
      { name: 'Section 1', order: 0, x: 0, y: 0, width: 50, height: 50 },
      { name: 'Section 2', order: 1, x: 50, y: 0, width: 50, height: 50 },
      { name: 'Section 3', order: 2, x: 0, y: 50, width: 50, height: 50 },
      { name: 'Section 4', order: 3, x: 50, y: 50, width: 50, height: 50 },
    ],
  },
  {
    id: 'six-grid',
    name: 'Six Grid',
    description: 'Six equal sections in 3x2 grid',
    width: 1920,
    height: 1080,
    orientation: 'LANDSCAPE',
    sections: [
      { name: 'Section 1', order: 0, x: 0, y: 0, width: 33.33, height: 50 },
      { name: 'Section 2', order: 1, x: 33.33, y: 0, width: 33.33, height: 50 },
      { name: 'Section 3', order: 2, x: 66.66, y: 0, width: 33.34, height: 50 },
      { name: 'Section 4', order: 3, x: 0, y: 50, width: 33.33, height: 50 },
      { name: 'Section 5', order: 4, x: 33.33, y: 50, width: 33.33, height: 50 },
      { name: 'Section 6', order: 5, x: 66.66, y: 50, width: 33.34, height: 50 },
    ],
  },
  {
    id: 'nine-grid',
    name: 'Nine Grid',
    description: 'Nine equal sections in 3x3 grid',
    width: 1920,
    height: 1080,
    orientation: 'LANDSCAPE',
    sections: [
      { name: 'Section 1', order: 0, x: 0, y: 0, width: 33.33, height: 33.33 },
      { name: 'Section 2', order: 1, x: 33.33, y: 0, width: 33.33, height: 33.33 },
      { name: 'Section 3', order: 2, x: 66.66, y: 0, width: 33.34, height: 33.33 },
      { name: 'Section 4', order: 3, x: 0, y: 33.33, width: 33.33, height: 33.33 },
      { name: 'Section 5', order: 4, x: 33.33, y: 33.33, width: 33.33, height: 33.33 },
      { name: 'Section 6', order: 5, x: 66.66, y: 33.33, width: 33.34, height: 33.33 },
      { name: 'Section 7', order: 6, x: 0, y: 66.66, width: 33.33, height: 33.34 },
      { name: 'Section 8', order: 7, x: 33.33, y: 66.66, width: 33.33, height: 33.34 },
      { name: 'Section 9', order: 8, x: 66.66, y: 66.66, width: 33.34, height: 33.34 },
    ],
  },
  {
    id: 'l-shape-left',
    name: 'L-Shape Left',
    description: 'L-shaped layout with main area and left sidebar',
    width: 1920,
    height: 1080,
    orientation: 'LANDSCAPE',
    sections: [
      { name: 'Main Content', order: 0, x: 30, y: 0, width: 70, height: 100 },
      { name: 'Left Top', order: 1, x: 0, y: 0, width: 30, height: 50 },
      { name: 'Left Bottom', order: 2, x: 0, y: 50, width: 30, height: 50 },
    ],
  },
  {
    id: 'l-shape-right',
    name: 'L-Shape Right',
    description: 'L-shaped layout with main area and right sidebar',
    width: 1920,
    height: 1080,
    orientation: 'LANDSCAPE',
    sections: [
      { name: 'Main Content', order: 0, x: 0, y: 0, width: 70, height: 100 },
      { name: 'Right Top', order: 1, x: 70, y: 0, width: 30, height: 50 },
      { name: 'Right Bottom', order: 2, x: 70, y: 50, width: 30, height: 50 },
    ],
  },
  {
    id: 'main-sidebar',
    name: 'Main + Side',
    description: 'Large main area with sidebar',
    width: 1920,
    height: 1080,
    orientation: 'LANDSCAPE',
    sections: [
      { name: 'Main Content', order: 0, x: 0, y: 0, width: 75, height: 100 },
      { name: 'Sidebar', order: 1, x: 75, y: 0, width: 25, height: 100 },
    ],
  },
  {
    id: 'main-top',
    name: 'Main + Top',
    description: 'Top banner with main content area',
    width: 1920,
    height: 1080,
    orientation: 'LANDSCAPE',
    sections: [
      { name: 'Top Banner', order: 0, x: 0, y: 0, width: 100, height: 20 },
      { name: 'Main Content', order: 1, x: 0, y: 20, width: 100, height: 80 },
    ],
  },
  {
    id: 'main-bottom',
    name: 'Main + Bottom',
    description: 'Main content with bottom banner',
    width: 1920,
    height: 1080,
    orientation: 'LANDSCAPE',
    sections: [
      { name: 'Main Content', order: 0, x: 0, y: 0, width: 100, height: 80 },
      { name: 'Bottom Banner', order: 1, x: 0, y: 80, width: 100, height: 20 },
    ],
  },
  // Scrolling text variants
  {
    id: 'main-with-scroll-bottom',
    name: 'Main + Bottom Scroll',
    description: 'Main content area with bottom scrolling text',
    width: 1920,
    height: 1080,
    orientation: 'LANDSCAPE',
    sections: [
      { name: 'Main Content', order: 0, x: 0, y: 0, width: 100, height: 85, type: 'media' },
      { name: 'Bottom Scroll Text', order: 1, x: 0, y: 85, width: 100, height: 15, type: 'text' },
    ],
  },
  {
    id: 'main-with-scroll-top',
    name: 'Main + Top Scroll',
    description: 'Main content area with top scrolling text',
    width: 1920,
    height: 1080,
    orientation: 'LANDSCAPE',
    sections: [
      { name: 'Top Scroll Text', order: 0, x: 0, y: 0, width: 100, height: 15, type: 'text' },
      { name: 'Main Content', order: 1, x: 0, y: 15, width: 100, height: 85, type: 'media' },
    ],
  },
  {
    id: 'main-with-scroll-side',
    name: 'Main + Side Scroll',
    description: 'Main content with side scrolling text',
    width: 1920,
    height: 1080,
    orientation: 'LANDSCAPE',
    sections: [
      { name: 'Main Content', order: 0, x: 0, y: 0, width: 80, height: 100, type: 'media' },
      { name: 'Side Scroll Text', order: 1, x: 80, y: 0, width: 20, height: 100, type: 'text' },
    ],
  },
  {
    id: 'dual-with-scroll',
    name: 'Dual + Bottom Scroll',
    description: 'Two content areas with bottom scrolling text',
    width: 1920,
    height: 1080,
    orientation: 'LANDSCAPE',
    sections: [
      { name: 'Content Left', order: 0, x: 0, y: 0, width: 50, height: 85, type: 'media' },
      { name: 'Content Right', order: 1, x: 50, y: 0, width: 50, height: 85, type: 'media' },
      { name: 'Bottom Scroll Text', order: 2, x: 0, y: 85, width: 100, height: 15, type: 'text' },
    ],
  },
  {
    id: 'triple-with-scroll',
    name: 'Triple + Scroll',
    description: 'Three content areas with scrolling text banner',
    width: 1920,
    height: 1080,
    orientation: 'LANDSCAPE',
    sections: [
      { name: 'Top Scroll Banner', order: 0, x: 0, y: 0, width: 100, height: 12, type: 'text' },
      { name: 'Content Left', order: 1, x: 0, y: 12, width: 33.33, height: 88, type: 'media' },
      { name: 'Content Center', order: 2, x: 33.33, y: 12, width: 33.33, height: 88, type: 'media' },
      { name: 'Content Right', order: 3, x: 66.66, y: 12, width: 33.34, height: 88, type: 'media' },
    ],
  },
  // Custom layout option
  {
    id: 'custom',
    name: 'Custom Layout',
    description: 'Create your own custom layout with drag & drop sections',
    width: 1920,
    height: 1080,
    orientation: 'LANDSCAPE',
    sections: [], // Empty sections - user will create their own
  },
];

interface LayoutTemplateSelectorProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSelectTemplate: (template: LayoutTemplate) => void;
}

export function LayoutTemplateSelector({
  open,
  onOpenChange,
  onSelectTemplate,
}: LayoutTemplateSelectorProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-5xl max-h-[90vh] overflow-y-auto bg-white">
        <DialogHeader>
          <DialogTitle className="text-2xl font-bold text-gray-900">SELECT LAYOUT TEMPLATE</DialogTitle>
          <DialogDescription className="text-base text-gray-600 mt-2">
            Choose a template to start with
          </DialogDescription>
        </DialogHeader>
        <div className="grid grid-cols-3 sm:grid-cols-4 gap-4 py-4">
          {LAYOUT_TEMPLATES.map((template) => (
            <button
              key={template.id}
              onClick={() => {
                onSelectTemplate(template);
                onOpenChange(false);
              }}
              className="group relative flex flex-col items-center justify-center p-4 border-2 border-gray-300 rounded-lg hover:border-blue-500 hover:shadow-lg transition-all cursor-pointer bg-white min-h-[140px]"
            >
              <div className="w-full space-y-3">
                <div
                  className="relative mx-auto border-2 border-gray-300 rounded bg-gray-50"
                  style={{
                    width: template.width > template.height ? '120px' : '60px',
                    height: template.width > template.height ? '68px' : '120px',
                    backgroundColor: '#f9fafb',
                  }}
                >
                  {template.sections.map((section, idx) => {
                    const sectionWidth = (section.width / 100) * (template.width > template.height ? 120 : 60);
                    const sectionHeight = (section.height / 100) * (template.width > template.height ? 68 : 120);
                    const sectionX = (section.x / 100) * (template.width > template.height ? 120 : 60);
                    const sectionY = (section.y / 100) * (template.width > template.height ? 68 : 120);
                    
                    return (
                      <div
                        key={idx}
                        className={`absolute border-2 rounded-sm ${
                          section.type === 'text' 
                            ? 'border-purple-500 bg-purple-100' 
                            : 'border-blue-500 bg-blue-100'
                        }`}
                        style={{
                          left: `${sectionX}px`,
                          top: `${sectionY}px`,
                          width: `${sectionWidth}px`,
                          height: `${sectionHeight}px`,
                        }}
                      />
                    );
                  })}
                </div>
                <div className="text-center px-2 min-h-[40px] flex items-center justify-center">
                  <span className="text-sm font-bold text-gray-900 group-hover:text-blue-700 leading-tight break-words">
                    {template.name}
                  </span>
                </div>
              </div>
            </button>
          ))}
        </div>
        <div className="flex justify-end pt-4 border-t border-gray-200">
          <Button 
            variant="outline" 
            onClick={() => onOpenChange(false)}
            className="font-semibold text-gray-900 border-gray-300 hover:bg-gray-50"
          >
            CANCEL
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}