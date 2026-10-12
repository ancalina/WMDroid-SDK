import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
import { satteri } from '@astrojs/markdown-satteri';
import search from './integrations/search.mjs';
import contracts, { baseLinks } from './integrations/contracts.mjs';

const base = process.env.WMDROID_DOCS_BASE || '/';
const site = process.env.WMDROID_DOCS_SITE;

export default defineConfig({
  publicDir: './images',
  base,
  ...(site ? { site } : {}),
  markdown: { processor: satteri({ hastPlugins: [baseLinks({ base })] }) },
  devToolbar: { enabled: false },
  redirects: { '/': `${base.replace(/\/$/, '')}/ko/` },
  integrations: [starlight({
    title: 'WMDroid SDK',
    pagefind: false,
    components: { Head: './src/components/Head.astro', Search: './src/components/Search.astro', PageTitle: './src/components/PageTitle.astro' },
    defaultLocale: 'ko',
    locales: {
      ko: { label: '한국어', lang: 'ko' },
      en: { label: 'English', lang: 'en' },
    },
    customCss: ['./src/styles/custom.css'],
    sidebar: [
  {
    "label": "Guides",
    "translations": {
      "ko": "개발 가이드"
    },
    "items": [
      {
        "label": "Overview",
        "translations": {
          "ko": "개요"
        },
        "slug": ""
      },
      {
        "label": "Getting started",
        "translations": {
          "ko": "시작하기"
        },
        "slug": "getting-started"
      },
      {
        "label": "Applets and shell",
        "translations": {
          "ko": "애플릿과 셸"
        },
        "slug": "applets"
      },
      {
        "label": "Today modules",
        "translations": {
          "ko": "Today 모듈"
        },
        "slug": "widgets"
      },
      {
        "label": "Shared UI",
        "translations": {
          "ko": "공통 UI"
        },
        "slug": "ui"
      },
      {
        "label": "Fonts and resources",
        "translations": {
          "ko": "글꼴과 리소스"
        },
        "slug": "resources"
      },
      {
        "label": "Verification",
        "translations": {
          "ko": "검증"
        },
        "slug": "verification"
      },
      {
        "label": "Writing rules",
        "translations": {
          "ko": "작성 규칙"
        },
        "slug": "style"
      },
      {
        "label": "Third-party notices",
        "translations": { "ko": "제3자 고지" },
        "slug": "licenses"
      }
    ]
  },
  {
    "label": "API reference",
    "translations": {
      "ko": "API 참조"
    },
    "items": [
      {
        "label": "API index",
        "translations": {
          "ko": "API 색인"
        },
        "slug": "api"
      },
      {
        "label": "Shell and windows",
          "collapsed": true,
        "translations": {
          "ko": "셸과 창"
        },
        "items": [
          {
            "slug": "api/wmapplet"
          },
          {
            "slug": "api/wmshell"
          },
          {
            "slug": "api/wmwindow"
          },
          {
            "slug": "api/wmviewport"
          },
          {
            "slug": "api/wmtitlebar"
          },
          {
            "slug": "api/wmbar"
          },
          {
            "slug": "api/wmcommandbar"
          },
          {
            "slug": "api/wmstartmenu"
          },
          {
            "slug": "api/wmstartbackground"
          },
          {
            "slug": "api/wmtrayslot"
          },
          {
            "slug": "api/wmtrayclock"
          },
          {
            "slug": "api/wmstatuspanel"
          },
          {
            "slug": "api/wmclockpanel"
          },
          {
            "slug": "api/wmcloseglyph"
          },
          {
            "slug": "api/wmsipbutton"
          },
          {
            "slug": "api/wmpointer"
          },
          {
            "slug": "api/wmdevice"
          }
        ]
      },
      {
        "label": "Today contracts and layout",
          "collapsed": true,
        "translations": {
          "ko": "Today 계약과 배치"
        },
        "items": [
          {
            "slug": "api/todaycontract"
          },
          {
            "slug": "api/todayprovider"
          },
          {
            "slug": "api/wmtoday"
          }
        ]
      },
      {
        "label": "Shared controls",
          "collapsed": true,
        "translations": {
          "ko": "공통 컨트롤"
        },
        "items": [
          {
            "slug": "api/wmcontrols"
          },
          {
            "slug": "api/wmchoice"
          },
          {
            "slug": "api/wmeditablechoice"
          },
          {
            "slug": "api/wmcategorypicker"
          },
          {
            "slug": "api/wmcategorynameform"
          },
          {
            "slug": "api/wmboxselect"
          },
          {
            "slug": "api/wmthreestatecheck"
          },
          {
            "slug": "api/wmscrollview"
          },
          {
            "slug": "api/wmscrollbar"
          },
          {
            "slug": "api/wmtabstrip"
          },
          {
            "slug": "api/wmtabdrawable"
          },
          {
            "slug": "api/wmpropertysheet"
          },
          {
            "slug": "api/wmlistsettingspane"
          },
          {
            "slug": "api/wmsettingsview"
          },
          {
            "slug": "api/wmiconcaption"
          }
        ]
      },
      {
        "label": "Menus and dialogs",
          "collapsed": true,
        "translations": {
          "ko": "메뉴와 대화상자"
        },
        "items": [
          {
            "slug": "api/wmmenuitem"
          },
          {
            "slug": "api/wmcommandmenu"
          },
          {
            "slug": "api/wmradiomenu"
          },
          {
            "slug": "api/wmpopup"
          },
          {
            "slug": "api/wmbubble"
          },
          {
            "slug": "api/wmdialog"
          },
          {
            "slug": "api/wmmessagebox"
          },
          {
            "slug": "api/wminputdialog"
          },
          {
            "slug": "api/wmwizard"
          }
        ]
      },
      {
        "label": "Fonts, resources, and rendering",
          "collapsed": true,
        "translations": {
          "ko": "글꼴, 리소스, 표시"
        },
        "items": [
          {
            "slug": "api/wmmetrics"
          },
          {
            "slug": "api/wmfonts"
          },
          {
            "slug": "api/wmfontfile"
          },
          {
            "slug": "api/wmresources"
          },
          {
            "slug": "api/wmresourcepack"
          },
          {
            "slug": "api/wmassets"
          },
          {
            "slug": "api/wmicons"
          },
          {
            "slug": "api/wmappicons"
          },
          {
            "slug": "api/wmiconpolicy"
          },
          {
            "slug": "api/wmlogo"
          },
          {
            "slug": "api/wmvectordrawable"
          },
          {
            "slug": "api/wmstrings"
          },
          {
            "slug": "api/wmcolor"
          },
          {
            "slug": "api/wmpalette"
          },
          {
            "slug": "api/wmpaint"
          },
          {
            "slug": "api/wmtouchfeedback"
          },
          {
            "slug": "api/wmtouchsoundoptions"
          },
          {
            "slug": "api/wmvolumefeedback"
          }
        ]
      },
      {
        "label": "Calendar, date, and time",
          "collapsed": true,
        "translations": {
          "ko": "일정, 날짜, 시간"
        },
        "items": [
          {
            "slug": "api/wmcalendarview"
          },
          {
            "slug": "api/wmcalendarheader"
          },
          {
            "slug": "api/wmagendaview"
          },
          {
            "slug": "api/wmmonthview"
          },
          {
            "slug": "api/wmmonthcalendar"
          },
          {
            "slug": "api/wmmonthcalendarheader"
          },
          {
            "slug": "api/wmyearview"
          },
          {
            "slug": "api/wmyearchooser"
          },
          {
            "slug": "api/wmcalendarpicker"
          },
          {
            "slug": "api/wmdatefield"
          },
          {
            "slug": "api/wmdatetimefield"
          },
          {
            "slug": "api/wmtimeformat"
          },
          {
            "slug": "api/wmtaskeditor"
          },
          {
            "slug": "api/wmappointmenteditor"
          },
          {
            "slug": "api/wmappointmenttext"
          },
          {
            "slug": "api/wmappointmentreminder"
          },
          {
            "slug": "api/wmreminder"
          },
          {
            "slug": "api/wmcalendarduration"
          },
          {
            "slug": "api/wmcalendarsymbols"
          },
          {
            "slug": "api/wmcalendargeneraloptions"
          },
          {
            "slug": "api/wmcalendarappointmentsoptions"
          },
          {
            "slug": "api/wmrecurrencerule"
          },
          {
            "slug": "api/wmrecurrencechoice"
          },
          {
            "slug": "api/wmrecurrenceeditor"
          }
        ]
      },
      {
        "label": "Phone and contacts",
          "collapsed": true,
        "translations": {
          "ko": "전화와 연락처"
        },
        "items": [
          {
            "slug": "api/wmphonekeypad"
          },
          {
            "slug": "api/wmphoneview"
          },
          {
            "slug": "api/wmspeeddialview"
          },
          {
            "slug": "api/wmcontactsview"
          },
          {
            "slug": "api/wmcontactsummaryview"
          },
          {
            "slug": "api/wmcontactdetailsview"
          },
          {
            "slug": "api/wmcontactsubcardview"
          }
        ]
      },
      {
        "label": "Documents and media",
          "collapsed": true,
        "translations": {
          "ko": "문서와 미디어"
        },
        "items": [
          {
            "slug": "api/wmnotesview"
          },
          {
            "slug": "api/wmpwidocument"
          },
          {
            "slug": "api/wmpwilayout"
          },
          {
            "slug": "api/wmpwitextlayout"
          },
          {
            "slug": "api/wmpwiview"
          },
          {
            "slug": "api/wmdocumentfolders"
          },
          {
            "slug": "api/wmdocumentproperties"
          },
          {
            "slug": "api/wmbrowserview"
          },
          {
            "slug": "api/wmbrowserfavoriteform"
          },
          {
            "slug": "api/wmbrowserfolderform"
          },
          {
            "slug": "api/wmbrowserfavoritesview"
          },
          {
            "slug": "api/wmmediaview"
          },
          {
            "slug": "api/wmmediaqueue"
          },
          {
            "slug": "api/wmmedialibraryview"
          },
          {
            "slug": "api/wmmediaplaylistview"
          },
          {
            "slug": "api/wmmediapropertiesview"
          }
        ]
      },
      {
        "label": "Internal implementation",
          "collapsed": true,
        "translations": {
          "ko": "내부 구현"
        },
        "items": [
          {
            "slug": "api/wmcharactersearch"
          },
          {
            "slug": "api/wmcomboglyph"
          },
          {
            "slug": "api/wmdateformats"
          },
          {
            "slug": "api/wmiconorder"
          },
          {
            "slug": "api/wmmonthyeareditor"
          }
        ]
      }
    ]
  }
],
  }), search(), contracts()],
});
