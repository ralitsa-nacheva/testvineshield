#!/usr/bin/env python3
"""Opaque sRGB theme-role checks only; not rendered accessibility certification."""
from pathlib import Path
import json,re
ROOT=Path(__file__).resolve().parents[1]
def run():
    text=(ROOT/'shared/src/commonMain/kotlin/com/rncoding/testvineshield/core/presentation/theme/VineShieldTheme.kt').read_text()
    light=text.split('private val light = lightColorScheme(',1)[1].split('private val dark',1)[0]
    dark=text.split('private val dark = darkColorScheme(',1)[1].split('@Composable',1)[0]
    def values(source):
        found={name:f'#{int(value,16)&0xFFFFFF:06X}' for name,value in re.findall(r'(?m)^\s*(\w+)\s*=\s*Color\((0x[0-9A-Fa-f]+)\)',source)}
        found.update({name:'#FFFFFF' for name in re.findall(r'(?m)^\s*(\w+)\s*=\s*Color.White',source)})
        return found
    def luminance(color):
        rgb=[int(color[i:i+2],16)/255 for i in (1,3,5)]
        rgb=[v/12.92 if v<=0.04045 else ((v+0.055)/1.055)**2.4 for v in rgb]
        return sum(a*b for a,b in zip(rgb,[0.2126,0.7152,0.0722]))
    pairs=[('onPrimary','primary'),('onPrimaryContainer','primaryContainer'),('onSecondary','secondary'),('onSecondaryContainer','secondaryContainer'),('onBackground','background'),('onSurface','surface'),('onSurfaceVariant','surfaceVariant')]
    results={}
    for name,source in [('light',light),('dark',dark)]:
        palette=values(source);rows=[]
        for fg,bg in pairs:
            high,low=sorted([luminance(palette[fg]),luminance(palette[bg])],reverse=True)
            ratio=(high+0.05)/(low+0.05)
            if ratio<4.5:raise SystemExit(f'Review explicit text-role contrast: {name} {fg}/{bg}')
            rows.append({'foreground_role':fg,'background_role':bg,'foreground':palette[fg],'background':palette[bg],'contrast_ratio':round(ratio,3)})
        results[name]=rows
    return {'status':'passed','scope':'14 explicit opaque sRGB text-role pairs only','minimum_ratio':4.5,'palettes':results,'not_checked':['rendered components','disabled/focused/pressed/error states','text sizing and layout','screen readers','remaining Material default color roles']}
if __name__=='__main__':print(json.dumps(run(),indent=2))
