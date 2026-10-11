---
title: "API reference"
slug: en/api
---

`me.ancal.wmdroid.sdk`

Choose a task below, then open the class reference for exact signatures and constraints. Build configuration is in [Getting started](/en/getting-started/).

| Task | Start with |
| --- | --- |
| Create an applet or compose a window | [WmApplet](/en/api/wmapplet/), [WmWindow](/en/api/wmwindow/), [WmViewport](/en/api/wmviewport/) |
| Provide Today data and custom content | [TodayProvider](/en/api/todayprovider/), [TodayContract](/en/api/todaycontract/), [WmToday](/en/api/wmtoday/) |
| Style Android Views and handle live changes | [WmControls](/en/api/wmcontrols/), [WmFonts](/en/api/wmfonts/), [WmStrings](/en/api/wmstrings/) |
| Edit dates, appointments, or recurrence drafts | [WmDateField](/en/api/wmdatefield/), [WmAppointmentEditor](/en/api/wmappointmenteditor/), [WmRecurrenceEditor](/en/api/wmrecurrenceeditor/) |
| Display contacts, documents, or media | [WmContactsView](/en/api/wmcontactsview/), [WmNotesView](/en/api/wmnotesview/), [WmMediaView](/en/api/wmmediaview/) |
| Install a pack or query its catalog | [WmResources](/en/api/wmresources/), [WmResourcePack](/en/api/wmresourcepack/), [WmAssets](/en/api/wmassets/) |

## Shell and windows

- [WmApplet](/en/api/wmapplet/): Activity base with a shared title bar, command bar, and Start menu.
- [WmShell](/en/api/wmshell/): Queries launcher appearance, Start menu, and resource state and creates shell-command Intents.
- [WmWindow](/en/api/wmwindow/): Container for the title, client, bottom commands, and popups on one logical screen.
- [WmViewport](/en/api/wmviewport/): Places one logical window on a physical screen with uniform scaling.
- [WmTitleBar](/en/api/wmtitlebar/): Logical-VGA title assembly containing Start, status slots, clock, and X/OK.
- [WmBar](/en/api/wmbar/): Drawable for title bars, command bars, and their button backgrounds.
- [WmCommandBar](/en/api/wmcommandbar/): Bottom bar for left and right commands and a central input-method area.
- [WmStartMenu](/en/api/wmstartmenu/): View for scrolling through folders and program entries and reporting selections.
- [WmStartBackground](/en/api/wmstartbackground/): Drawable that tiles a Start-menu Bitmap and draws a black border.
- [WmTraySlot](/en/api/wmtrayslot/): Title-bar slot combining status artwork, touch target, and connection animation.
- [WmTrayClock](/en/api/wmtrayclock/): Digital/analog title-bar clock.
- [WmStatusPanel](/en/api/wmstatuspanel/): Popup body for Android connection, volume, battery, or phone status.
- [WmClockPanel](/en/api/wmclockpanel/): Read-only popup body showing date, time, battery, and two appointment lines supplied by the owner.
- [WmCloseGlyph](/en/api/wmcloseglyph/): Provides the title bar OK or X glyph and pressed color.
- [WmSipButton](/en/api/wmsipbutton/): Input-method toggle and picker for an eligible editor in the content.
- [WmPointer](/en/api/wmpointer/): Finger-mouse View for relative motion and tap clicks within a logical surface.
- [WmDevice](/en/api/wmdevice/): Reads airplane mode, SIM, cellular service, and active network state.

## Today contracts and layout

- [TodayContract](/en/api/todaycontract/): v1 URI, column, metadata, and custom-view definitions shared by Today providers and the launcher.
- [TodayProvider](/en/api/todayprovider/): ContentProvider base for cached public Today rows and optional RemoteViews.
- [WmToday](/en/api/wmtoday/): Today module layout, row text, appointment selection, and rasterization for RemoteViews.

## Shared controls

- [WmControls](/en/api/wmcontrols/): Functions for applying shared fonts, themes, languages, button styles, and modal behavior to Android Views.
- [WmChoice](/en/api/wmchoice/): Closed single-selection field linked to a caller-owned popup.
- [WmEditableChoice](/en/api/wmeditablechoice/): Field combining direct editing, candidate selection, and a caller-owned popup.
- [WmCategoryPicker](/en/api/wmcategorypicker/): Checked category-selection draft and new-name validation; the owner manages the catalog, confirmation, cancellation, and persistence.
- [WmCategoryNameForm](/en/api/wmcategorynameform/): Measured New Category input frame, client heading and divider; the owner manages acceptance and storage.
- [WmBoxSelect](/en/api/wmboxselect/): Manages single or multiple text-cell selections with a bit mask.
- [WmThreeStateCheck](/en/api/wmthreestatecheck/): CheckBox that cycles through unchecked, checked, and indeterminate states.
- [WmScrollView](/en/api/wmscrollview/): Combines finger scrolling with a WM scrollbar.
- [WmScrollBar](/en/api/wmscrollbar/): Vertical scrollbar that controls a Host pixel offset.
- [WmTabStrip](/en/api/wmtabstrip/): Places bottom tabs and overflow arrows below the content.
- [WmTabDrawable](/en/api/wmtabdrawable/): Draws selected or unselected bottom-tab backgrounds and borders.
- [WmPropertySheet](/en/api/wmpropertysheet/): Manages retained pages, bottom tabs, selection, and Apply or Cancel gates.
- [WmListSettingsPane](/en/api/wmlistsettingspane/): Settings container that lays out a list and a right command column.
- [WmSettingsView](/en/api/wmsettingsview/): Builds settings tabs and icon grids from owner-supplied categories and artwork.
- [WmIconCaption](/en/api/wmiconcaption/): TextView that paints a selection rectangle around visible caption ink, measured by Android Layout.

## Menus and dialogs

- [WmMenuItem](/en/api/wmmenuitem/): Immutable data for commands, checks, separators, and submenus.
- [WmCommandMenu](/en/api/wmcommandmenu/): Scrollable command popup with submenus anchored to a bottom command.
- [WmRadioMenu](/en/api/wmradiomenu/): Provides rows and keyboard behavior for command, radio, and choice lists.
- [WmPopup](/en/api/wmpopup/): Places the title and content inside a tailed popup.
- [WmBubble](/en/api/wmbubble/): Draws a bubble popup with a white body, gradient header, tail, and outline.
- [WmDialog](/en/api/wmdialog/): Embedded dialog container that selects the default button from focus and handles Enter and Escape.
- [WmMessageBox](/en/api/wmmessagebox/): Modal dialog for acknowledgment or a Yes/No choice.
- [WmInputDialog](/en/api/wminputdialog/): Full-client dialog for a single name field and Done or Cancel commands.
- [WmWizard](/en/api/wmwizard/): Manages retained step pages with Cancel, Back, Next, and Finish commands.

## Fonts, resources, and rendering

- [WmMetrics](/en/api/wmmetrics/): Logical-pixel dimensions and height calculations for shared controls before scaling.
- [WmFonts](/en/api/wmfonts/): Loads fonts, links hybrid fallback faces, attaches TextViews, and measures Canvas text.
- [WmFontFile](/en/api/wmfontfile/): Stores imported fonts under SHA-256 names and resolves launcher font URIs.
- [WmResources](/en/api/wmresources/): Resolves the app resource pack, launcher snapshot, and asset fallbacks.
- [WmResourcePack](/en/api/wmresourcepack/): Validates and installs version 2 ZIP resource packs and exposes immutable snapshots.
- [WmAssets](/en/api/wmassets/): Queries resource-pack catalog entries and opens original files or previews.
- [WmIcons](/en/api/wmicons/): Loads status icons from the active resource pack or bundled fallbacks.
- [WmAppIcons](/en/api/wmappicons/): Renders Android app icons into square Bitmaps.
- [WmIconPolicy](/en/api/wmiconpolicy/): Value object for per-Activity adaptive-icon selection and hidden-app lists.
- [WmLogo](/en/api/wmlogo/): Drawable for Start-button artwork within the assigned bounds.
- [WmVectorDrawable](/en/api/wmvectordrawable/): Reads pack v1 vector JSON into a Drawable and draws it within its bounds.
- [WmStrings](/en/api/wmstrings/): Looks up strings and language fallbacks in i18n JSON catalogs.
- [WmColor](/en/api/wmcolor/): WM-style HSB conversion and hue changes for existing Bitmaps.
- [WmPalette](/en/api/wmpalette/): Functions for deriving highlight and control-face colors and interpolating ARGB.
- [WmPaint](/en/api/wmpaint/): Container that reveals a client area from top to bottom using the old-device behavior settings.
- [WmTouchFeedback](/en/api/wmtouchfeedback/): Asynchronous SoundPool playback of pack soft/loud touch WAV files.
- [WmTouchSoundOptions](/en/api/wmtouchsoundoptions/): Controls for enabling touch sound and selecting soft/loud playback.
- [WmVolumeFeedback](/en/api/wmvolumefeedback/): Media and ring preview sounds selected by volume-control step.

## Calendar, date, and time

- [WmCalendarView](/en/api/wmcalendarview/): Displays day/5-day/6-day/week grids and selection details.
- [WmCalendarHeader](/en/api/wmcalendarheader/): View-dependent Calendar toolbar.
- [WmAgendaView](/en/api/wmagendaview/): Displays one day as all-day rows, a time strip, and sorted two-line timed rows.
- [WmMonthView](/en/api/wmmonthview/): Calendar month view with 42 dates, touch highlighting, and appointment decorations.
- [WmLunarCalendar](/en/api/wmlunarcalendar/): Converts civil dates to Korean lunar dates and captions from optional resource-pack data.
- [WmMonthCalendar](/en/api/wmmonthcalendar/): 42-cell date-popup body, distinct from Calendar month view.
- [WmMonthCalendarHeader](/en/api/wmmonthcalendarheader/): Month/year title and navigation buttons for a date dropdown, separate from the Calendar toolbar.
- [WmYearView](/en/api/wmyearview/): Calendar year view.
- [WmYearChooser](/en/api/wmyearchooser/): Embedded year-input dialog View for 1900..2999.
- [WmCalendarPicker](/en/api/wmcalendarpicker/): Date dropdown combining month title, calendar, month list, and inline year editor.
- [WmDateField](/en/api/wmdatefield/): Nullable segmented date-only View.
- [WmDateTimeField](/en/api/wmdatetimefield/): Combines a segmented date field and editable time combo.
- [WmTimeFormat](/en/api/wmtimeformat/): Builds Calendar details and minute-level time captions.
- [WmTaskEditor](/en/api/wmtaskeditor/): Task/Notes editor with nullable dates, priority, completion, categories and sensitivity.
- [WmAppointmentEditor](/en/api/wmappointmenteditor/): Two-page Appointment/Notes editor for subject, location, start/end, all-day, recurrence, reminder, busy status, and sensitivity.
- [WmAppointmentText](/en/api/wmappointmenttext/): Applies NUL termination and empty-subject fallback at the appointment save boundary.
- [WmAppointmentReminder](/en/api/wmappointmentreminder/): Displays relative reminder enablement, quantity, and unit.
- [WmReminder](/en/api/wmreminder/): Immutable relative appointment reminder.
- [WmCalendarDuration](/en/api/wmcalendarduration/): Converts appointment-editor durations and CalendarProvider `DURATION`.
- [WmCalendarSymbols](/en/api/wmcalendarsymbols/): Appointment-metadata Drawable.
- [WmCalendarGeneralOptions](/en/api/wmcalendargeneraloptions/): Calendar settings page for start view, first weekday, work-week columns, half-hour rows, and week numbers.
- [WmCalendarAppointmentsOptions](/en/api/wmcalendarappointmentsoptions/): Edits default new-appointment reminder, display symbols, and meeting-request account.
- [WmRecurrenceRule](/en/api/wmrecurrencerule/): Immutable recurrence pattern and end range.
- [WmRecurrenceChoice](/en/api/wmrecurrencechoice/): Combo for once/weekly/monthly/yearly presets derived from start date, plus Edit Pattern.
- [WmRecurrenceEditor](/en/api/wmrecurrenceeditor/): Provider-free recurrence wizard for time/pattern/end range; all-day omits the time page.

## Phone and contacts

- [WmPhoneKeypad](/en/api/wmphonekeypad/): 4×4 telephone keypad.
- [WmPhoneView](/en/api/wmphoneview/): Client for number entry, status, voicemail, and keypad.
- [WmSpeedDialView](/en/api/wmspeeddialview/): List of speed-dial slots and contact names.
- [WmContactsView](/en/api/wmcontactsview/): Displays contact search, ASCII initial groups, and contact rows.
- [WmContactSummaryView](/en/api/wmcontactsummaryview/): Displays a contact name, company, job title, phone/link/address actions, and a read-only Notes tab.
- [WmContactDetailsView](/en/api/wmcontactdetailsview/): Edits contact drafts in Details and Notes tabs.
- [WmContactSubcardView](/en/api/wmcontactsubcardview/): Edits individual components of a contact name or address.

## Documents and media

- [WmNotesView](/en/api/wmnotesview/): Client for a document list, folder filtering, sorting, and plain-text editing.
- [WmPwiDocument](/en/api/wmpwidocument/): Immutable reader model for plain-text storage and recovered binary PWI text records.
- [WmPwiLayout](/en/api/wmpwilayout/): Recovered RichInk coordinate conversion, indentation, and line-position arithmetic.
- [WmPwiTextLayout](/en/api/wmpwitextlayout/): Native wrapping for PWI text tokens using owner-supplied measurement.
- [WmPwiView](/en/api/wmpwiview/): Read-only binary PWI text view using recovered wrapping/coordinate kernels with Android font measurement.
- [WmDocumentFolders](/en/api/wmdocumentfolders/): Flat document-folder list with New, Rename, and Delete commands.
- [WmDocumentProperties](/en/api/wmdocumentproperties/): Form for renaming a document and choosing its folder/storage destination.
- [WmBrowserView](/en/api/wmbrowserview/): Browser client containing an address editor and Android `WebView`.
- [WmBrowserFavoriteForm](/en/api/wmbrowserfavoriteform/): Form for a favorite name, address, and destination.
- [WmBrowserFolderForm](/en/api/wmbrowserfolderform/): Name form for a favorites folder.
- [WmBrowserFavoritesView](/en/api/wmbrowserfavoritesview/): Displays folders and links in one list.
- [WmMediaView](/en/api/wmmediaview/): Audio/video client backed by Android MediaPlayer, with a queue, Now Playing, Library, and Properties panels.
- [WmMediaQueue](/en/api/wmmediaqueue/): Ordered playback queue that permits duplicate sources.
- [WmMediaLibraryView](/en/api/wmmedialibraryview/): Displays an owner-supplied media hierarchy with titled icon rows.
- [WmMediaPlaylistView](/en/api/wmmediaplaylistview/): Now Playing list with selection and Move Up/Down, Add, Remove, and Properties toolbar actions.
- [WmMediaPropertiesView](/en/api/wmmediapropertiesview/): Displays a media metadata snapshot as label/value pairs.

## Internal implementation

These classes are package-private. Use the linked public API.

- [WmCharacterSearch](/en/api/wmcharactersearch/): Package-private character-search state for choice fields and popup lists.
- [WmComboGlyph](/en/api/wmcomboglyph/): Package-private arrow, popup binding, and key routing for choice fields.
- [WmDateFormats](/en/api/wmdateformats/): Package-private date-format implementation; not directly callable.
- [WmIconOrder](/en/api/wmiconorder/): Internal icon-list insertion comparator.
- [WmMonthYearEditor](/en/api/wmmonthyeareditor/): Package-private inline year editor managed by [WmCalendarPicker](/en/api/wmcalendarpicker/).

## Similar control names

| Purpose | API |
| --- | --- |
| Calendar month view and event decoration | [WmMonthView](/en/api/wmmonthview/) |
| 42-cell body in a date-selection popup | [WmMonthCalendar](/en/api/wmmonthcalendar/) |
| Picker combining month header, grid, month list and year input | [WmCalendarPicker](/en/api/wmcalendarpicker/) |
| Fixed choice, editable candidates and cell selection | [Shared UI selection guide](/en/ui/#control-selection) |
